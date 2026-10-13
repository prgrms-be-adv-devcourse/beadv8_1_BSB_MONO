package com.bukang.member.app;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bukang.jwt.JwtProperties;
import com.bukang.jwt.JwtProvider;
import com.bukang.member.out.RefreshTokenRepository;

// 같은 family일 때만 지우는 판단은 refresh-token-delete-family.lua가 한다 (RefreshTokenRepositoryTest)
class LogoutUseCaseTest {
	private static final JwtProperties PROPERTIES = new JwtProperties(
		"jwt-test-secret-key-0000-0000-0000-0000", Duration.ofMinutes(30), Duration.ofDays(14));
	private static final Instant NOW = Instant.parse("2026-10-13T00:00:00Z");
	private static final long MEMBER_ID = 7;

	private JwtProvider jwtProvider;
	private RefreshTokenRepository refreshTokenRepository;
	private LogoutUseCase logoutUseCase;

	@BeforeEach
	void setUp() {
		jwtProvider = new JwtProvider(PROPERTIES, Clock.fixed(NOW, ZoneOffset.UTC));
		refreshTokenRepository = mock(RefreshTokenRepository.class);
		logoutUseCase = new LogoutUseCase(jwtProvider, refreshTokenRepository);
	}

	@Test
	@DisplayName("토큰의 familyId로 Redis 기록 삭제를 요청한다")
	void deletesByFamily() {
		String refreshToken = jwtProvider.issueRefreshToken(MEMBER_ID, "family-1", NOW.plus(Duration.ofDays(14)));

		logoutUseCase.logout(refreshToken);

		verify(refreshTokenRepository).deleteIfFamily(MEMBER_ID, "family-1");
	}

	@Test
	@DisplayName("토큰이 없거나 유효하지 않으면 아무것도 지우지 않고 끝난다")
	void ignoreMissingOrInvalidToken() {
		logoutUseCase.logout(null);
		logoutUseCase.logout(" ");
		logoutUseCase.logout("not-a-jwt");
		logoutUseCase.logout(jwtProvider.issueAccessToken(MEMBER_ID));

		verify(refreshTokenRepository, never()).deleteIfFamily(anyLong(), anyString());
	}
}
