package com.bukang.member.in;

import java.net.URI;
import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bukang.common.global.rsdata.RsData;
import com.bukang.member.app.MemberFacade;
import com.bukang.member.dto.AuthTokens;
import com.bukang.member.dto.MemberDto;
import com.bukang.member.dto.MemberJoinRequestDto;
import com.bukang.member.dto.MemberLoginRequestDto;
import com.bukang.member.dto.MemberLoginResponseDto;
import com.bukang.member.dto.MemberLoginResult;
import com.bukang.member.dto.TokenReissueResponseDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Auth", description = "회원가입, 로그인, 토큰 재발급, 로그아웃 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class ApiV1AuthController {
	// Refresh Token 쿠키는 토큰 재발급 등 인증 API에만 실려 가도록 경로를 좁힌다
	static final String REFRESH_TOKEN_COOKIE = "refreshToken";
	static final String REFRESH_TOKEN_COOKIE_PATH = "/api/v1/auth";

	private final MemberFacade memberFacade;
	private final SecurityContextRepository securityContextRepository;

	// ResponseEntity.created()의 201은 springdoc이 추론하지 못하므로 응답 코드를 직접 명시한다
	// 에러 응답은 GlobalExceptionHandler가 RsData(data: null) 형식으로 반환한다
	@PostMapping("/join")
	@Operation(summary = "회원가입", description = "이메일, 닉네임, 휴대폰 번호 중복을 검사한 뒤 회원을 생성한다.")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "가입 성공",
			content = @Content(examples = @ExampleObject(value = AuthApiExamples.JOIN_SUCCESS))),
		@ApiResponse(responseCode = "400", description = "입력값 검증 실패 또는 요청 본문 형식 오류",
			content = @Content(schema = @Schema(implementation = RsData.class), examples = {
				@ExampleObject(name = "입력값 검증 실패", value = AuthApiExamples.JOIN_INVALID_INPUT),
				@ExampleObject(name = "요청 본문 형식 오류", value = AuthApiExamples.INVALID_BODY)
			})),
		@ApiResponse(responseCode = "409", description = "이메일, 닉네임 또는 휴대폰 번호 중복",
			content = @Content(schema = @Schema(implementation = RsData.class), examples = {
				@ExampleObject(name = "이메일 중복", value = AuthApiExamples.DUPLICATE_EMAIL),
				@ExampleObject(name = "닉네임 중복", value = AuthApiExamples.DUPLICATE_NICKNAME),
				@ExampleObject(name = "휴대폰 번호 중복", value = AuthApiExamples.DUPLICATE_PHONE)
			}))
	})
	public ResponseEntity<RsData<MemberDto>> join(
		@Valid @RequestBody MemberJoinRequestDto memberJoinDto
	) {
		MemberDto memberDto = memberFacade.join(memberJoinDto);
		return ResponseEntity.created(URI.create("/api/v1/member/members/" + memberDto.getId()))
			.body(RsData.of(HttpStatus.CREATED, "Member Joined Successfully", memberDto));
	}

	@PostMapping("/login")
	@Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고 Access Token은 응답 본문으로, "
		+ "Refresh Token은 httpOnly 쿠키로 내려준다. Refresh Token은 Redis에도 저장한다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "로그인 성공",
			headers = @Header(name = HttpHeaders.SET_COOKIE,
				description = "refreshToken=...; Path=/api/v1/auth; Max-Age=1209600; Secure; HttpOnly; SameSite=Lax"),
			content = @Content(examples = @ExampleObject(value = AuthApiExamples.LOGIN_SUCCESS))),
		@ApiResponse(responseCode = "400", description = "입력값 검증 실패 또는 요청 본문 형식 오류",
			content = @Content(schema = @Schema(implementation = RsData.class), examples = {
				@ExampleObject(name = "입력값 검증 실패", value = AuthApiExamples.LOGIN_INVALID_INPUT),
				@ExampleObject(name = "요청 본문 형식 오류", value = AuthApiExamples.INVALID_BODY)
			})),
		@ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = AuthApiExamples.LOGIN_FAILED)))
	})
	public ResponseEntity<RsData<MemberLoginResponseDto>> login(
		@Valid @RequestBody MemberLoginRequestDto memberLoginRequestDto,
		HttpServletRequest request,
		HttpServletResponse response
	) {
		MemberLoginResult result = memberFacade.login(memberLoginRequestDto);

		// 게이트웨이 토큰 검사(PRO-20)가 붙기 전까지는 기존 API가 세션으로 로그인 상태를 확인하므로 세션 저장도 유지한다
		SecurityContext context = SecurityContextHolder.getContext();
		securityContextRepository.saveContext(context, request, response);

		AuthTokens tokens = result.tokens();
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE,
				refreshTokenCookie(tokens.refreshToken(), tokens.refreshTokenExpiration()).toString())
			.body(RsData.of(HttpStatus.OK, "로그인 성공",
				new MemberLoginResponseDto(tokens.accessToken(), result.member())));
	}

	@PostMapping("/reissue")
	@Operation(summary = "토큰 재발급", description = "쿠키의 Refresh Token을 검증하고 Redis에 저장된 지금 토큰이면 "
		+ "새 Access Token은 응답 본문으로, 새 Refresh Token은 쿠키로 내려준다(회전). "
		+ "이미 쓴 토큰이 다시 오면 탈취로 보고 그 로그인을 끊는다. 단, 회전 직후 5초 안에 바로 전 토큰이 오면 "
		+ "지금 Refresh Token을 그대로 다시 내려준다(동시 요청·응답 유실 대비).")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "재발급 성공",
			headers = @Header(name = HttpHeaders.SET_COOKIE,
				description = "refreshToken=...; Path=/api/v1/auth; Max-Age=1209600; Secure; HttpOnly; SameSite=Lax "
					+ "(Max-Age는 토큰 만료까지 남은 초. 로그인 30일 상한에 가까우면 14일보다 짧다)"),
			content = @Content(examples = @ExampleObject(value = AuthApiExamples.REISSUE_SUCCESS))),
		@ApiResponse(responseCode = "401", description = "Refresh Token 쿠키 없음, 만료 또는 유효하지 않음",
			content = @Content(schema = @Schema(implementation = RsData.class), examples = {
				@ExampleObject(name = "쿠키 없음", value = AuthApiExamples.REISSUE_MISSING_TOKEN),
				@ExampleObject(name = "만료", value = AuthApiExamples.REISSUE_EXPIRED_TOKEN),
				@ExampleObject(name = "유효하지 않음", value = AuthApiExamples.REISSUE_INVALID_TOKEN)
			}))
	})
	public ResponseEntity<RsData<TokenReissueResponseDto>> reissue(
		@Parameter(description = "로그인 때 받은 Refresh Token (httpOnly 쿠키라 브라우저가 자동으로 보낸다)")
		@CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken
	) {
		AuthTokens tokens = memberFacade.reissue(refreshToken);
		// 회전한 Refresh Token을 같은 이름·경로의 쿠키로 내려줘 브라우저가 이전 쿠키를 바꿔 끼우게 한다
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE,
				refreshTokenCookie(tokens.refreshToken(), tokens.refreshTokenExpiration()).toString())
			.body(RsData.of(HttpStatus.OK, "토큰 재발급 성공", new TokenReissueResponseDto(tokens.accessToken())));
	}

	@PostMapping("/logout")
	@Operation(summary = "로그아웃", description = "Redis의 Refresh Token을 지우고 Refresh Token 쿠키를 만료시킨다. "
		+ "쿠키가 없거나 이미 무효한 토큰이어도 성공으로 응답한다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "로그아웃 성공",
			headers = @Header(name = HttpHeaders.SET_COOKIE,
				description = "refreshToken=; Path=/api/v1/auth; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; "
					+ "Secure; HttpOnly; SameSite=Lax"),
			content = @Content(examples = @ExampleObject(value = AuthApiExamples.LOGOUT_SUCCESS)))
	})
	public ResponseEntity<RsData<Void>> logout(
		@Parameter(description = "로그인 때 받은 Refresh Token (httpOnly 쿠키라 브라우저가 자동으로 보낸다)")
		@CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
		HttpServletRequest request,
		HttpServletResponse response
	) {
		memberFacade.logout(refreshToken);

		// 게이트웨이 토큰 검사(PRO-20) 전까지 로그인이 세션에도 저장되므로 세션도 함께 끝낸다
		new SecurityContextLogoutHandler().logout(request, response, null);

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, refreshTokenCookie("", Duration.ZERO).toString())
			.body(RsData.of(HttpStatus.OK, "로그아웃 성공", null));
	}

	// httpOnly: 자바스크립트에서 읽지 못하게 해 XSS로 토큰이 새는 것을 막는다
	// SameSite=Lax: 다른 사이트에서 보낸 POST에는 쿠키가 실리지 않는다
	// maxAge가 0이면 브라우저가 쿠키를 지운다 (로그아웃). 지우려면 이름·경로가 발급할 때와 같아야 한다
	private ResponseCookie refreshTokenCookie(String value, Duration maxAge) {
		return ResponseCookie.from(REFRESH_TOKEN_COOKIE, value)
			.httpOnly(true)
			.secure(true)
			.sameSite("Lax")
			.path(REFRESH_TOKEN_COOKIE_PATH)
			.maxAge(maxAge)
			.build();
	}
}
