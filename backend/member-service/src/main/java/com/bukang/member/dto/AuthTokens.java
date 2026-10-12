package com.bukang.member.dto;

import java.time.Duration;

/**
 * 발급한 토큰. refreshTokenExpiration은 Refresh Token 쿠키의 Max-Age에 쓴다.
 */
public record AuthTokens(String accessToken, String refreshToken, Duration refreshTokenExpiration) {
}
