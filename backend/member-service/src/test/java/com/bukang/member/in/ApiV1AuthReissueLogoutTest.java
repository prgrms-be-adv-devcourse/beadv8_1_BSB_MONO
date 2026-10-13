package com.bukang.member.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.bukang.jwt.JwtProvider;
import com.bukang.jwt.TokenType;
import com.bukang.member.app.MemberFacade;
import com.bukang.member.dto.MemberJoinRequestDto;
import com.bukang.member.out.MemberRepository;
import com.bukang.member.out.RefreshTokenRepository;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

// 테스트에서는 Redis 서버를 띄우지 않으므로 RefreshTokenRepository를 메모리 Map으로 흉내 낸다
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiV1AuthReissueLogoutTest {
	private static final String EMAIL = "runner1@crewrun.com";
	private static final String PASSWORD = "password1234";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberFacade memberFacade;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private RefreshTokenRepository refreshTokenRepository;

	private final Map<Long, String> redis = new HashMap<>();
	private int memberId;

	@BeforeEach
	void setUp() {
		willAnswer(invocation -> redis.put(invocation.getArgument(0), invocation.getArgument(1)))
			.given(refreshTokenRepository).save(anyLong(), anyString(), any(Duration.class));
		willAnswer(invocation -> Optional.ofNullable(redis.get(invocation.<Long>getArgument(0))))
			.given(refreshTokenRepository).findByMemberId(anyLong());
		willAnswer(invocation -> redis.remove(invocation.<Long>getArgument(0)))
			.given(refreshTokenRepository).delete(anyLong());

		memberId = memberFacade.join(new MemberJoinRequestDto(
			EMAIL, PASSWORD, "김러너", "러너1", "010-1234-5678", LocalDate.of(1995, 4, 12), null
		)).getId();
	}

	@AfterEach
	void tearDown() {
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("유효한 Refresh Token 쿠키로 새 Access Token을 받는다")
	void reissue() throws Exception {
		String refreshToken = loginAndGetRefreshToken();

		MvcResult result = reissue(refreshToken)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("토큰 재발급 성공"))
			.andReturn();

		String accessToken = JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
		assertThat(jwtProvider.parse(accessToken, TokenType.ACCESS).memberId()).isEqualTo(memberId);
	}

	@Test
	@DisplayName("Refresh Token 쿠키가 없으면 401이다")
	void reissueWithoutCookie() throws Exception {
		mockMvc.perform(post("/api/v1/auth/reissue"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("Refresh Token이 없습니다. 다시 로그인해주세요."));
	}

	@Test
	@DisplayName("위조된 Refresh Token이면 401이다")
	void reissueWithInvalidToken() throws Exception {
		reissue("not-a-jwt")
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("유효하지 않은 Refresh Token입니다. 다시 로그인해주세요."));
	}

	@Test
	@DisplayName("로그아웃하면 Redis의 토큰이 지워지고 쿠키가 만료되며, 이후 재발급은 401이다")
	void logoutThenReissueRejected() throws Exception {
		String refreshToken = loginAndGetRefreshToken();

		MvcResult result = mockMvc.perform(post("/api/v1/auth/logout")
				.cookie(new Cookie("refreshToken", refreshToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("로그아웃 성공"))
			.andReturn();

		assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
			.startsWith("refreshToken=;")
			.contains("Path=/api/v1/auth", "Max-Age=0", "HttpOnly");
		assertThat(redis).doesNotContainKey((long)memberId);

		reissue(refreshToken)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("유효하지 않은 Refresh Token입니다. 다시 로그인해주세요."));
	}

	@Test
	@DisplayName("쿠키 없이 로그아웃해도 성공하고 쿠키 만료를 내려준다")
	void logoutWithoutCookie() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/logout"))
			.andExpect(status().isOk())
			.andReturn();

		assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
	}

	private String loginAndGetRefreshToken() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{ "email": "%s", "password": "%s" }
					""".formatted(EMAIL, PASSWORD)))
			.andExpect(status().isOk())
			.andReturn();
		return result.getResponse().getCookie("refreshToken").getValue();
	}

	private ResultActions reissue(String refreshToken) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/reissue").cookie(new Cookie("refreshToken", refreshToken)));
	}
}
