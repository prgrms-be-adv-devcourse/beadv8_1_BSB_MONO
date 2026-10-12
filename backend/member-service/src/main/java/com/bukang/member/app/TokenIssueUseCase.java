package com.bukang.member.app;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.bukang.jwt.JwtProperties;
import com.bukang.jwt.JwtProvider;
import com.bukang.member.dto.AuthTokens;
import com.bukang.member.out.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenIssueUseCase {
	private final JwtProvider jwtProvider;
	private final JwtProperties jwtProperties;
	private final RefreshTokenRepository refreshTokenRepository;

	// Refresh Token을 Redis에도 저장해, 재발급 때 서버가 마지막으로 발급한 토큰인지 대조할 수 있게 한다
	public AuthTokens issue(long memberId) {
		String accessToken = jwtProvider.issueAccessToken(memberId);
		String refreshToken = jwtProvider.issueRefreshToken(memberId);
		saveRefreshToken(memberId, refreshToken);

		return new AuthTokens(accessToken, refreshToken, jwtProperties.refreshTokenExpiration());
	}

	// Redis 장애로 저장하지 못해도 로그인은 막지 않는다.
	// 재발급은 Redis에 저장된 토큰과 같아야만 통과하므로, 저장되지 않은 Refresh Token은 재발급에 쓸 수 없다.
	// 그래서 보안은 그대로이고, 사용자는 Access Token이 만료되면(30분) 다시 로그인하면 된다.
	private void saveRefreshToken(long memberId, String refreshToken) {
		try {
			refreshTokenRepository.save(memberId, refreshToken, jwtProperties.refreshTokenExpiration());
		} catch (DataAccessException e) {
			// 토큰 값은 로그에 남기지 않는다
			log.warn("Refresh Token을 Redis에 저장하지 못했습니다. 이 로그인은 재발급할 수 없습니다. memberId={}", memberId, e);
		}
	}
}
