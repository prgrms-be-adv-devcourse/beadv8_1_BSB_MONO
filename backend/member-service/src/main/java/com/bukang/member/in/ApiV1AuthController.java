package com.bukang.member.in;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
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

import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "Auth", description = "회원가입, 로그인 API")
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
			.header(HttpHeaders.SET_COOKIE, refreshTokenCookie(tokens).toString())
			.body(RsData.of(HttpStatus.OK, "로그인 성공",
				new MemberLoginResponseDto(tokens.accessToken(), result.member())));
	}

	// httpOnly: 자바스크립트에서 읽지 못하게 해 XSS로 토큰이 새는 것을 막는다
	// SameSite=Lax: 다른 사이트에서 보낸 POST에는 쿠키가 실리지 않는다
	private ResponseCookie refreshTokenCookie(AuthTokens tokens) {
		return ResponseCookie.from(REFRESH_TOKEN_COOKIE, tokens.refreshToken())
			.httpOnly(true)
			.secure(true)
			.sameSite("Lax")
			.path(REFRESH_TOKEN_COOKIE_PATH)
			.maxAge(tokens.refreshTokenExpiration())
			.build();
	}
}
