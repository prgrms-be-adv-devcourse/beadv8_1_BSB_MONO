package com.bukang.member.app;

import org.springframework.stereotype.Service;

import com.bukang.jwt.InvalidTokenException;
import com.bukang.jwt.JwtClaims;
import com.bukang.jwt.JwtProvider;
import com.bukang.jwt.TokenType;
import com.bukang.member.out.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LogoutUseCase {
	private final JwtProvider jwtProvider;
	private final RefreshTokenRepository refreshTokenRepository;

	// Redis의 Refresh Token을 지워 더 이상 재발급되지 않게 한다.
	// 쿠키가 없거나 만료·위조된 토큰이면 지울 것이 없으므로 그냥 넘어간다 (로그아웃은 항상 성공).
	// Redis 값과 다른 토큰(다른 기기에서 다시 로그인해 덮어써진 이전 토큰)이면, 지금 로그인한 기기가 로그아웃되지 않도록 지우지 않는다.
	public void logout(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			return;
		}

		JwtClaims claims;
		try {
			claims = jwtProvider.parse(refreshToken, TokenType.REFRESH);
		} catch (InvalidTokenException e) {
			return;
		}

		long memberId = claims.memberId();
		boolean matches = refreshTokenRepository.findByMemberId(memberId)
			.map(refreshToken::equals)
			.orElse(false);
		if (matches) {
			refreshTokenRepository.delete(memberId);
		}
	}
}
