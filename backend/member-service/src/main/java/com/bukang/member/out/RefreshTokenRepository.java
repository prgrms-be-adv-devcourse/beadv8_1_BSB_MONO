package com.bukang.member.out;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * 회원별 Refresh Token 저장소 (Redis).
 * 회원당 하나만 보관하므로, 다시 로그인하면 이전 Refresh Token은 덮어써진다.
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {
	private final StringRedisTemplate redisTemplate;

	// 키 형식: members:{회원 ID}:refreshToken (.claude/rules/redis-key-convention.md)
	static String key(long memberId) {
		return "members:" + memberId + ":refreshToken";
	}

	// 토큰이 만료되는 시점에 Redis에서도 지워지도록 토큰 유효 기간을 TTL로 건다
	public void save(long memberId, String refreshToken, Duration ttl) {
		redisTemplate.opsForValue().set(key(memberId), refreshToken, ttl);
	}

	public Optional<String> findByMemberId(long memberId) {
		return Optional.ofNullable(redisTemplate.opsForValue().get(key(memberId)));
	}
}
