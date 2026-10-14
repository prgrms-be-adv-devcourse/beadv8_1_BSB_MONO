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

	// Redis의 Refresh Token 기록(family)을 지워 더 이상 재발급되지 않게 한다.
	// 쿠키가 없거나 만료·위조된 토큰이면 지울 것이 없으므로 그냥 넘어간다 (로그아웃은 항상 성공).
	// 지금 family의 토큰일 때만 지운다. 다른 기기에서 다시 로그인해 family가 바뀌었으면, 지금 로그인한 기기가 로그아웃되지 않도록 남긴다.
	// 같은 family라면 회전으로 이미 바뀐 이전 토큰이어도 지운다 (같은 로그인을 끝내는 것이므로).
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

		// 회전 도입 전에 발급된 토큰에는 familyId가 없다. 지울 family를 알 수 없으므로 넘어간다
		if (claims.familyId() == null) {
			return;
		}
		refreshTokenRepository.deleteIfFamily(claims.memberId(), claims.familyId());
	}
}
