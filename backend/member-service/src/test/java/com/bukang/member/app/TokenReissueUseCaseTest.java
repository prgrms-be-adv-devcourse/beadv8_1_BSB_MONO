package com.bukang.member.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bukang.jwt.JwtProperties;
import com.bukang.jwt.JwtProvider;
import com.bukang.jwt.TokenType;
import com.bukang.member.domain.exception.InvalidRefreshTokenException;
import com.bukang.member.out.RefreshTokenRepository;

class TokenReissueUseCaseTest {
	private static final JwtProperties PROPERTIES = new JwtProperties(
		"jwt-test-secret-key-0000-0000-0000-0000", Duration.ofMinutes(30), Duration.ofDays(14));
	private static final Instant NOW = Instant.parse("2026-10-13T00:00:00Z");
	private static final long MEMBER_ID = 7;

	private JwtProvider jwtProvider;
	private RefreshTokenRepository refreshTokenRepository;
	private TokenReissueUseCase tokenReissueUseCase;

	@BeforeEach
	void setUp() {
		jwtProvider = new JwtProvider(PROPERTIES, Clock.fixed(NOW, ZoneOffset.UTC));
		refreshTokenRepository = mock(RefreshTokenRepository.class);
		tokenReissueUseCase = new TokenReissueUseCase(jwtProvider, refreshTokenRepository);
	}

	@Test
	@DisplayName("Redis에 저장된 Refresh Token과 같으면 새 Access Token을 발급한다")
	void reissue() {
		String refreshToken = jwtProvider.issueRefreshToken(MEMBER_ID);
		given(refreshTokenRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(refreshToken));

		String accessToken = tokenReissueUseCase.reissue(refreshToken);

		assertThat(jwtProvider.parse(accessToken, TokenType.ACCESS).memberId()).isEqualTo(MEMBER_ID);
	}

	@Test
	@DisplayName("Refresh Token이 없으면 거절한다")
	void rejectMissingToken() {
		assertThatThrownBy(() -> tokenReissueUseCase.reissue(null))
			.isInstanceOf(InvalidRefreshTokenException.class)
			.hasMessage(TokenReissueUseCase.MISSING_MESSAGE);
	}

	@Test
	@DisplayName("만료된 Refresh Token은 만료 메시지로 거절한다")
	void rejectExpiredToken() {
		Clock fifteenDaysAgo = Clock.fixed(NOW.minus(Duration.ofDays(15)), ZoneOffset.UTC);
		JwtProvider pastProvider = new JwtProvider(PROPERTIES, fifteenDaysAgo);
		String expiredToken = pastProvider.issueRefreshToken(MEMBER_ID);

		assertThatThrownBy(() -> tokenReissueUseCase.reissue(expiredToken))
			.isInstanceOf(InvalidRefreshTokenException.class)
			.hasMessage(TokenReissueUseCase.EXPIRED_MESSAGE);
	}

	@Test
	@DisplayName("Access Token을 Refresh Token 자리에 보내면 거절한다")
	void rejectAccessToken() {
		String accessToken = jwtProvider.issueAccessToken(MEMBER_ID);

		assertThatThrownBy(() -> tokenReissueUseCase.reissue(accessToken))
			.isInstanceOf(InvalidRefreshTokenException.class)
			.hasMessage(TokenReissueUseCase.INVALID_MESSAGE);
	}

	@Test
	@DisplayName("Redis에 토큰이 없으면(로그아웃) 거절한다")
	void rejectWhenNotStored() {
		String refreshToken = jwtProvider.issueRefreshToken(MEMBER_ID);
		given(refreshTokenRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> tokenReissueUseCase.reissue(refreshToken))
			.isInstanceOf(InvalidRefreshTokenException.class)
			.hasMessage(TokenReissueUseCase.INVALID_MESSAGE);
	}

	@Test
	@DisplayName("Redis 값과 다르면(다른 기기에서 다시 로그인해 덮어써짐) 거절한다")
	void rejectWhenOverwritten() {
		JwtProvider earlierProvider = new JwtProvider(PROPERTIES, Clock.fixed(NOW.minusSeconds(60), ZoneOffset.UTC));
		String oldToken = earlierProvider.issueRefreshToken(MEMBER_ID);
		given(refreshTokenRepository.findByMemberId(MEMBER_ID))
			.willReturn(Optional.of(jwtProvider.issueRefreshToken(MEMBER_ID)));

		assertThatThrownBy(() -> tokenReissueUseCase.reissue(oldToken))
			.isInstanceOf(InvalidRefreshTokenException.class)
			.hasMessage(TokenReissueUseCase.INVALID_MESSAGE);
	}
}
