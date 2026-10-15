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
 * 순번 조회는 읽기만 하므로 Lua 없이 명령을 하나씩 보낸다.
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
				String.valueOf(epochMillis(now)),
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

	/**
	 * 회원이 대회·종목의 줄 어디에 있는지 읽는다. waiting에 있으면 순번을, 없으면 입장(active)했는지 본다.
	 * 명령 사이에 승격이 끼어들어도 다음 조회에서 바로잡힌다.
	 * waiting을 먼저 읽어야 방금 승격된 회원을 어디에도 없는 회원으로 잘못 보지 않는다.
	 */
	public Position findPosition(int raceId, int raceCourseId, int memberId, LocalDateTime now) {
		String member = String.valueOf(memberId);
		try {
			// 전체 인원을 순번보다 먼저 읽는다. 그 사이의 승격은 내 순번을 줄이기만 해서 순번이 전체보다 커지지 않는다
			Long total = redisTemplate.opsForZSet().size(waitingKey(raceId, raceCourseId));
			Long rank = redisTemplate.opsForZSet().rank(waitingKey(raceId, raceCourseId), member);
			if (rank != null) {
				return Position.waiting(rank, total);
			}
			// active 끝 시각이 지났으면 스케줄러가 지우기 전이라도 입장한 것으로 보지 않는다
			Double activeUntil = redisTemplate.opsForZSet().score(activeKey(raceId, raceCourseId), member);
			if (activeUntil != null && activeUntil > epochMillis(now)) {
				return Position.active();
			}
			return Position.none();
		} catch (DataAccessException exception) {
			log.error("대기열 순번을 Redis에서 읽지 못했습니다. raceId={}, raceCourseId={}, memberId={}",
				raceId, raceCourseId, memberId, exception);
			throw new QueueUnavailableException();
		}
	}

	private static String queueKey(int raceId, int raceCourseId) {
		return "race:" + raceId + ":racecourse:" + raceCourseId;
	}

	// 등록 시각·active 끝 시각을 score로 쓰는 epoch ms로 바꾼다
	private static long epochMillis(LocalDateTime time) {
		return time.atZone(ZONE).toInstant().toEpochMilli();
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

	/**
	 * 순번 조회 결과.
	 *
	 * @param state 줄에서 기다리는 중(WAITING), 입장함(ACTIVE), 어디에도 없음(NONE)
	 * @param rank 줄에서 내 앞에 있는 인원 (0부터). WAITING이 아니면 0
	 * @param total 줄 전체 인원. WAITING이 아니면 0
	 */
	public record Position(State state, long rank, long total) {
		static Position waiting(long rank, long total) {
			return new Position(State.WAITING, rank, total);
		}

		static Position active() {
			return new Position(State.ACTIVE, 0, 0);
		}

		static Position none() {
			return new Position(State.NONE, 0, 0);
		}

		public enum State {
			WAITING, ACTIVE, NONE
		}
	}
}
