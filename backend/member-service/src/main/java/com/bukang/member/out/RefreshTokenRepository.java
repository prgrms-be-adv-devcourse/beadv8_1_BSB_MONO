package com.bukang.member.out;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * 회원별 Refresh Token 기록 저장소 (Redis Hash).
 * 회원당 한 family만 보관하므로, 다시 로그인하면 이전 family는 덮어써진다.
 *
 * <p>Hash 칸: familyId, currentJti, currentToken, previousJti, rotatedAt(초), familyExpiresAt(초).
 * 키 TTL은 지금 토큰의 만료 시각이라, 그동안 재발급이 없으면 기록이 저절로 사라진다.
 * 확인하고 바꾸는 일은 src/main/resources/redis/의 Lua 스크립트가 Redis 안에서 한 번에 한다.
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {
	private static final RedisScript<Long> CREATE = script("refresh-token-create.lua", Long.class);
	@SuppressWarnings("rawtypes")
	private static final RedisScript<List> ROTATE = script("refresh-token-rotate.lua", List.class);
	private static final RedisScript<Long> DELETE_FAMILY = script("refresh-token-delete-family.lua", Long.class);

	private static final String FAMILY_EXPIRES_AT = "familyExpiresAt";

	private final StringRedisTemplate redisTemplate;

	// 키 형식: members:{회원 ID}:refreshToken (.claude/rules/redis-key-convention.md)
	static String key(long memberId) {
		return "members:" + memberId + ":refreshToken";
	}

	/**
	 * 로그인: 새 family 기록을 만든다. 이전 family가 있으면 덮어쓴다.
	 */
	public void create(long memberId, NewFamily family) {
		redisTemplate.execute(CREATE, List.of(key(memberId)),
			family.familyId(),
			family.jti(),
			family.token(),
			seconds(family.issuedAt()),
			seconds(family.familyExpiresAt()),
			seconds(family.tokenExpiresAt()));
	}

	/**
	 * 재발급: 들어온 토큰을 판정하고, 지금 토큰이면 새 토큰으로 바꾼다.
	 * 새 토큰은 판정 전에 미리 만들어 넘긴다. ROTATED가 아니면 그 토큰은 쓰이지 않는다.
	 */
	public RotationResult rotate(long memberId, Rotation rotation) {
		List<?> result = redisTemplate.execute(ROTATE, List.of(key(memberId)),
			rotation.familyId(),
			rotation.presentedJti(),
			rotation.newJti(),
			rotation.newToken(),
			seconds(rotation.now()),
			String.valueOf(rotation.reuseGrace().toSeconds()),
			seconds(rotation.newTokenExpiresAt()));
		RotationOutcome outcome = RotationOutcome.valueOf((String)result.get(0));
		String currentToken = outcome == RotationOutcome.GRACE ? (String)result.get(1) : null;
		return new RotationResult(outcome, currentToken);
	}

	/**
	 * 로그아웃: familyId가 지금 family일 때만 기록을 지운다.
	 *
	 * @return 지웠으면 true
	 */
	public boolean deleteIfFamily(long memberId, String familyId) {
		Long deleted = redisTemplate.execute(DELETE_FAMILY, List.of(key(memberId)), familyId);
		return deleted != null && deleted > 0;
	}

	/**
	 * 회전할 새 토큰의 만료를 30일 상한에서 자르려고 family 최종 만료를 읽는다.
	 * family마다 한 번 정해지고 바뀌지 않으므로 판정(rotate)과 따로 읽어도 된다.
	 */
	public Optional<Instant> findFamilyExpiresAt(long memberId) {
		Object value = redisTemplate.opsForHash().get(key(memberId), FAMILY_EXPIRES_AT);
		return Optional.ofNullable(value).map(v -> Instant.ofEpochSecond(Long.parseLong((String)v)));
	}

	// JWT의 iat·exp와 같은 초 단위로 저장한다
	private static String seconds(Instant instant) {
		return String.valueOf(instant.getEpochSecond());
	}

	private static <T> RedisScript<T> script(String fileName, Class<T> resultType) {
		DefaultRedisScript<T> script = new DefaultRedisScript<>();
		script.setLocation(new ClassPathResource("redis/" + fileName));
		script.setResultType(resultType);
		return script;
	}

	/**
	 * 로그인 때 만드는 family 기록.
	 */
	public record NewFamily(
		String familyId, String jti, String token, Instant issuedAt, Instant tokenExpiresAt, Instant familyExpiresAt
	) {
		@Override
		public String toString() {
			return "NewFamily[familyId=" + familyId + ", jti=" + jti + ", token=***]";
		}
	}

	/**
	 * 재발급 판정에 넘기는 값.
	 *
	 * @param familyId 들어온 토큰의 familyId
	 * @param presentedJti 들어온 토큰의 jti
	 * @param newJti 정상일 때 저장할 새 토큰의 jti
	 * @param newToken 정상일 때 저장할 새 토큰 원본
	 */
	public record Rotation(
		String familyId, String presentedJti, String newJti, String newToken,
		Instant now, Duration reuseGrace, Instant newTokenExpiresAt
	) {
		@Override
		public String toString() {
			return "Rotation[familyId=" + familyId + ", presentedJti=" + presentedJti + ", newJti=" + newJti
				+ ", newToken=***]";
		}
	}
}
