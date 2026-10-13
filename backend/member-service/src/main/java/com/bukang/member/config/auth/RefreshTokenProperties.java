package com.bukang.member.config.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Refresh Token 회전 정책 (member.auth.refresh-token.*).
 * 회전은 member-service만 하므로 공용 jwt.* 가 아니라 여기에 둔다. 한 번에 늘리는 기간(14일)은 공용 jwt.refresh-token-expiration을 쓴다.
 *
 * @param reuseGrace 회전 직후 바로 전 토큰을 탈취로 보지 않고 받아 주는 시간. 탭 두 개·동시 요청·응답 유실을 살린다
 * @param maxLifetime 로그인부터 회전을 이어 갈 수 있는 최대 기간. 계속 써도 이 기간이 지나면 다시 로그인한다
 */
@ConfigurationProperties("member.auth.refresh-token")
public record RefreshTokenProperties(
	@DefaultValue("5s") Duration reuseGrace,
	@DefaultValue("30d") Duration maxLifetime
) {

	public RefreshTokenProperties {
		if (reuseGrace == null || reuseGrace.isNegative()) {
			throw new IllegalArgumentException("member.auth.refresh-token.reuse-grace는 0 이상이어야 합니다: " + reuseGrace);
		}
		if (maxLifetime == null || maxLifetime.isNegative() || maxLifetime.isZero()) {
			throw new IllegalArgumentException("member.auth.refresh-token.max-lifetime은 0보다 커야 합니다: " + maxLifetime);
		}
	}
}
