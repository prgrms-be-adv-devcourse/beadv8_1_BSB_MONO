package com.bukang.member.app;

import org.springframework.stereotype.Service;

import com.bukang.jwt.InvalidTokenException;
import com.bukang.jwt.JwtClaims;
import com.bukang.jwt.JwtProvider;
import com.bukang.jwt.TokenType;
import com.bukang.member.domain.exception.InvalidRefreshTokenException;
import com.bukang.member.out.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenReissueUseCase {
	static final String MISSING_MESSAGE = "Refresh Token이 없습니다. 다시 로그인해주세요.";
	static final String EXPIRED_MESSAGE = "Refresh Token이 만료되었습니다. 다시 로그인해주세요.";
	static final String INVALID_MESSAGE = "유효하지 않은 Refresh Token입니다. 다시 로그인해주세요.";

	private final JwtProvider jwtProvider;
	private final RefreshTokenRepository refreshTokenRepository;

	// 서명·만료를 검사한 뒤, Redis에 저장된(서버가 마지막으로 발급한) 토큰과 같을 때만 새 Access Token을 발급한다.
	// 로그아웃했거나 다른 기기에서 다시 로그인해 덮어써진 토큰은 Redis 값과 달라 거절된다.
	// Refresh Token은 바꾸지 않는다(회전 없음). 로그인할 때 받은 토큰을 만료(14일) 또는 로그아웃까지 계속 쓴다.
	public String reissue(String refreshToken) {
		long memberId = verify(refreshToken);
		return jwtProvider.issueAccessToken(memberId);
	}

	private long verify(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new InvalidRefreshTokenException(MISSING_MESSAGE);
		}

		JwtClaims claims;
		try {
			claims = jwtProvider.parse(refreshToken, TokenType.REFRESH);
		} catch (InvalidTokenException e) {
			if (e.getReason() == InvalidTokenException.Reason.EXPIRED) {
				throw new InvalidRefreshTokenException(EXPIRED_MESSAGE);
			}
			throw new InvalidRefreshTokenException(INVALID_MESSAGE);
		}

		boolean matches = refreshTokenRepository.findByMemberId(claims.memberId())
			.map(refreshToken::equals)
			.orElse(false);
		if (!matches) {
			throw new InvalidRefreshTokenException(INVALID_MESSAGE);
		}
		return claims.memberId();
	}
}
