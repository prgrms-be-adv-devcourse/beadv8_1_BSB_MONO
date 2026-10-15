package com.bukang.queue.out;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import com.bukang.queue.domain.exception.QueueUnavailableException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 대기열 줄 저장소 (Redis).
 * 대회·종목마다 줄 두 개를 둔다: waiting(ZSet, score는 등록 시각)과 active(ZSet, score는 활성 시간 끝 시각).
 * 스케줄러가 승격할 대기열은 queue:waiting(Set)에 {raceId}:{raceCourseId}로 모은다.
 * 확인하고 넣는 일은 src/main/resources/redis/의 Lua 스크립트가 Redis 안에서 한 번에 한다.
 * Redis에 연결하지 못하면 대기열 없이 통과시키지 않고 503으로 막는다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class QueueRedisRepository {
	@SuppressWarnings("rawtypes")
	private static final RedisScript<List> REGISTER = script("queue-register.lua", List.class);

	// 스케줄러가 승격할 대기열 목록 (값: {raceId}:{raceCourseId})
	static final String QUEUES_KEY = "queue:waiting";
	// score(epoch ms)와 LocalDateTime을 바꿀 때 쓰는 시간대. 복제본의 접수 일정(상품 JVM 시각)과 같은 시간대다
	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
	private static final String ACTIVE = "ACTIVE";

	private final StringRedisTemplate redisTemplate;

	// 키 형식: race:{raceId}:racecourse:{raceCourseId}:waiting (.claude/rules/redis-key-convention.md)
	static String waitingKey(int raceId, int raceCourseId) {
		return queueKey(raceId, raceCourseId) + ":waiting";
	}

	// 키 형식: race:{raceId}:racecourse:{raceCourseId}:active
	static String activeKey(int raceId, int raceCourseId) {
		return queueKey(raceId, raceCourseId) + ":active";
	}

	// queue:waiting에 넣는 대기열 값
	static String queueId(int raceId, int raceCourseId) {
		return raceId + ":" + raceCourseId;
	}

	/**
	 * 회원을 대회·종목의 waiting에 넣는다. 이미 줄에 있으면 처음 순번을 유지하고, 이미 입장(active)했으면 넣지 않는다.
	 */
	public Registration register(int raceId, int raceCourseId, int memberId, LocalDateTime now) {
		List<?> result;
		try {
			result = redisTemplate.execute(REGISTER,
				List.of(waitingKey(raceId, raceCourseId), activeKey(raceId, raceCourseId), QUEUES_KEY),
				String.valueOf(memberId),
				epochMillis(now),
				queueId(raceId, raceCourseId));
		} catch (DataAccessException exception) {
			log.error("대기열 등록을 Redis에서 처리하지 못했습니다. raceId={}, raceCourseId={}, memberId={}",
				raceId, raceCourseId, memberId, exception);
			throw new QueueUnavailableException();
		}
		if (ACTIVE.equals(result.get(0))) {
			return Registration.alreadyActive();
		}
		return Registration.waiting((Long)result.get(1), (Long)result.get(2));
	}

	private static String queueKey(int raceId, int raceCourseId) {
		return "race:" + raceId + ":racecourse:" + raceCourseId;
	}

	// 등록 시각·active 끝 시각을 score로 쓰는 epoch ms로 바꾼다
	private static String epochMillis(LocalDateTime time) {
		return String.valueOf(time.atZone(ZONE).toInstant().toEpochMilli());
	}

	private static <T> RedisScript<T> script(String fileName, Class<T> resultType) {
		DefaultRedisScript<T> script = new DefaultRedisScript<>();
		script.setLocation(new ClassPathResource("redis/" + fileName));
		script.setResultType(resultType);
		return script;
	}

	/**
	 * 등록 결과.
	 *
	 * @param active 이미 입장(active)한 회원이라 줄에 넣지 않았으면 true (이때 rank, total은 0)
	 * @param rank 줄에서 내 앞에 있는 인원 (0부터)
	 * @param total 줄 전체 인원
	 */
	public record Registration(boolean active, long rank, long total) {
		static Registration alreadyActive() {
			return new Registration(true, 0, 0);
		}

		static Registration waiting(long rank, long total) {
			return new Registration(false, rank, total);
		}
	}
}
