package com.bukang.member.dto;

import java.time.Duration;

/**
 * 발급한 토큰. refreshTokenExpiration은 Refresh Token 쿠키의 Max-Age에 쓴다.
 */
public record AuthTokens(String accessToken, String refreshToken, Duration refreshTokenExpiration) {
	// record 기본 toString()은 모든 값을 출력하므로, 실수로 로그에 찍혀도 토큰이 새지 않게 가린다
	@Override
	public String toString() {
		return "AuthTokens[accessToken=***, refreshToken=***, refreshTokenExpiration=" + refreshTokenExpiration + "]";
	}
}
