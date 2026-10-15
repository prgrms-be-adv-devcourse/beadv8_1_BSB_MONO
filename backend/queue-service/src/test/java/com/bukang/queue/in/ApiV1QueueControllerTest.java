package com.bukang.queue.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.bukang.common.shared.race.domain.RaceSaleStatus;
import com.bukang.queue.domain.QueueRace;
import com.bukang.queue.domain.QueueRaceCourse;
import com.bukang.queue.dto.QueueRegisterRequestDto;
import com.bukang.queue.support.QueueIntegrationTest;

/**
 * 대기열 등록 API(POST /api/v1/queue)를 실제 Redis 컨테이너와 H2 복제본으로 확인한다.
 * 등록 시각은 Facade가 실제 지금으로 만들므로, 복제본의 접수 일정도 실제 지금을 기준으로 잡는다.
 * 승격 스케줄러가 아직 없어, active 회원은 active ZSet에 끝 시각을 직접 넣어 만든다.
 */
class ApiV1QueueControllerTest extends QueueIntegrationTest {
	private static final int RACE_ID = 1;
	private static final int OTHER_RACE_ID = 2;
	private static final int RACE_COURSE_ID = 11;
	private static final int MEMBER_ID = 7;
	private static final int OTHER_MEMBER_ID = 8;
	private static final String WAITING_KEY = "race:1:racecourse:11:waiting";
	private static final String ACTIVE_KEY = "race:1:racecourse:11:active";
	private static final String QUEUES_KEY = "queue:waiting";
	// QueueRedisRepository가 score(epoch ms)를 만들 때 쓰는 시간대
	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("register: 처음 등록한 회원은 1번이고, 그 종목의 waiting과 스케줄러가 돌 대기열 목록(queue:waiting)에 들어간다")
	void registerFirstMember() throws Exception {
		saveOpenRaceAndCourse();

		register(MEMBER_ID)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("대기열에 등록했습니다."))
			.andExpect(jsonPath("$.data.myRank").value(1))
			.andExpect(jsonPath("$.data.ahead").value(0))
			.andExpect(jsonPath("$.data.total").value(1))
			.andExpect(jsonPath("$.data.canEnter").value(false));

		assertThat(redisTemplate.opsForZSet().score(WAITING_KEY, "7")).isNotNull();
		assertThat(redisTemplate.opsForSet().isMember(QUEUES_KEY, "1:11")).isTrue();
	}

	@Test
	@DisplayName("register: 다른 기기에서 1시간 전에 줄을 선 회원이 다시 등록하면, 처음 등록 시각(score)이 바뀌지 않아 "
		+ "순번을 그대로 유지한다 (NX)")
	void keepFirstPositionWhenSameMemberRegistersAgain() throws Exception {
		saveOpenRaceAndCourse();
		LocalDateTime now = LocalDateTime.now();
		double firstScore = epochMillis(now.minusHours(1));
		redisTemplate.opsForZSet().add(WAITING_KEY, "7", firstScore);

		register(MEMBER_ID)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.myRank").value(1))
			.andExpect(jsonPath("$.data.ahead").value(0))
			.andExpect(jsonPath("$.data.total").value(1));

		assertThat(redisTemplate.opsForZSet().score(WAITING_KEY, "7")).isEqualTo(firstScore);
	}

	@Test
	@DisplayName("register: 다른 회원이 뒤이어 등록하면 2번이고 앞에 1명이 있다")
	void registerNextMember() throws Exception {
		saveOpenRaceAndCourse();
		register(MEMBER_ID);

		register(OTHER_MEMBER_ID)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.myRank").value(2))
			.andExpect(jsonPath("$.data.ahead").value(1))
			.andExpect(jsonPath("$.data.total").value(2));
	}

	@Test
	@DisplayName("register: 이미 입장한 회원(active 끝 시각이 10분 뒤)이 다시 등록하면 409이고 waiting에 넣지 않는다")
	void rejectActiveMember() throws Exception {
		saveOpenRaceAndCourse();
		redisTemplate.opsForZSet().add(ACTIVE_KEY, "7", epochMillis(LocalDateTime.now().plusMinutes(10)));

		register(MEMBER_ID)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("이미 입장한 회원입니다."));

		assertThat(redisTemplate.hasKey(WAITING_KEY)).isFalse();
	}

	@Test
	@DisplayName("register: active 끝 시각이 1분 전에 지난 회원은 스케줄러가 지우기 전이라도 다시 줄을 설 수 있다")
	void registerMemberWhoseActiveEnded() throws Exception {
		// 뭔 상황인지 이해는 하지만 예제로썬 2% 부족한 코드
		saveOpenRaceAndCourse();
		redisTemplate.opsForZSet().add(ACTIVE_KEY, "7", epochMillis(LocalDateTime.now().minusMinutes(1)));

		register(MEMBER_ID)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.myRank").value(1));

		assertThat(redisTemplate.opsForZSet().score(WAITING_KEY, "7")).isNotNull();
	}

	@Test
	@DisplayName("register: 판매 예정(SCHEDULED)인 대회는 개인 예매 시간 안이어도 403이고 waiting에 넣지 않는다")
	void rejectScheduledRace() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		saveRace(RACE_ID, RaceSaleStatus.SCHEDULED, now.minusHours(1), now.plusDays(1));
		saveCourse(RACE_ID, false);

		register(MEMBER_ID)
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("접수 기간이 아닙니다."));

		assertThat(redisTemplate.hasKey(WAITING_KEY)).isFalse();
	}

	@Test
	@DisplayName("register: 판매 중이어도 개인 예매 오픈(1시간 뒤) 전이면 403이다")
	void rejectBeforeIndividualOpen() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		saveRace(RACE_ID, RaceSaleStatus.ON_SALE, now.plusHours(1), now.plusDays(1));
		saveCourse(RACE_ID, false);

		register(MEMBER_ID)
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("접수 기간이 아닙니다."));

		assertThat(redisTemplate.hasKey(WAITING_KEY)).isFalse();
	}

	@Test
	@DisplayName("register: 판매 중이어도 접수 마감(1시간 전) 뒤면 403이다 (판매 상태는 대회일 다음 날에야 CLOSED로 바뀐다)")
	void rejectAfterApplicationClose() throws Exception {
		// 접수 마감 시간 뒤여도 ON_SALE인 타이밍이 있다 (판매 상태는 대회일 다음 날 0시에 CLOSED로 바뀐다)
		LocalDateTime now = LocalDateTime.now();
		saveRace(RACE_ID, RaceSaleStatus.ON_SALE, now.minusDays(2), now.minusHours(1));
		saveCourse(RACE_ID, false);

		register(MEMBER_ID)
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("접수 기간이 아닙니다."));

		assertThat(redisTemplate.hasKey(WAITING_KEY)).isFalse();
	}

	@Test
	@DisplayName("register: 접수 중인 대회 1에 아직 열리지 않은 대회 2의 종목으로 등록하면 403이다 "
		+ "(대회 1의 상태 판정은 통과하므로, 종목이 대회와 맞는지 확인하지 않으면 없는 조합의 줄이 생긴다)")
	void rejectCourseOfOtherRace() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		saveRace(RACE_ID, RaceSaleStatus.ON_SALE, now.minusHours(1), now.plusDays(1));
		saveRace(OTHER_RACE_ID, RaceSaleStatus.ON_SALE, now.plusHours(1), now.plusDays(1));
		saveCourse(OTHER_RACE_ID, false);

		register(MEMBER_ID)
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("이 대회의 종목이 아닙니다."));

		assertThat(redisTemplate.hasKey(WAITING_KEY)).isFalse();
	}

	@Test
	@DisplayName("register: 대회에서 뺀 종목(삭제 표시)으로 등록하면 403이다")
	void rejectDeletedCourse() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		saveRace(RACE_ID, RaceSaleStatus.ON_SALE, now.minusHours(1), now.plusDays(1));
		saveCourse(RACE_ID, true);

		register(MEMBER_ID)
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("이 대회의 종목이 아닙니다."));

		assertThat(redisTemplate.hasKey(WAITING_KEY)).isFalse();
	}

	@Test
	@DisplayName("register: 복제본에 없는 대회면 404이다")
	void rejectRaceNotFound() throws Exception {
		register(MEMBER_ID)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("존재하지 않는 대회입니다."));
	}

	@Test
	@DisplayName("register: 복제본에 없는 종목이면 404이다")
	void rejectCourseNotFound() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		saveRace(RACE_ID, RaceSaleStatus.ON_SALE, now.minusHours(1), now.plusDays(1));

		register(MEMBER_ID)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("존재하지 않는 종목입니다."));
	}

	@Test
	@DisplayName("register: 회원 ID 헤더가 없으면 401이다")
	void rejectWithoutMemberIdHeader() throws Exception {
		saveOpenRaceAndCourse();

		mockMvc.perform(post("/api/v1/queue")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody()))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("register: 대회 ID가 없으면 400이다")
	void rejectWithoutRaceId() throws Exception {
		mockMvc.perform(post("/api/v1/queue")
				.header(MemberIdHeader.NAME, MEMBER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"raceCourseId\":11}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("대회 ID를 작성해주세요."));
	}

	// 대회 1·종목 11로 등록한다
	private ResultActions register(int memberId) throws Exception {
		return mockMvc.perform(post("/api/v1/queue")
			.header(MemberIdHeader.NAME, memberId)
			.contentType(MediaType.APPLICATION_JSON)
			.content(requestBody()));
	}

	private String requestBody() {
		return jsonConverter.toJson(new QueueRegisterRequestDto(RACE_ID, RACE_COURSE_ID));
	}

	// 지금 접수 중인 대회 1(판매 중, 1시간 전 개인 예매 오픈, 하루 뒤 접수 마감)과 그 종목 11
	private void saveOpenRaceAndCourse() {
		LocalDateTime now = LocalDateTime.now();
		saveRace(RACE_ID, RaceSaleStatus.ON_SALE, now.minusHours(1), now.plusDays(1));
		saveCourse(RACE_ID, false);
	}

	private void saveRace(
		int raceId,
		RaceSaleStatus saleStatus,
		LocalDateTime individualOpenAt,
		LocalDateTime applicationCloseAt
	) {
		queueRaceRepository.save(new QueueRace(raceId, saleStatus, individualOpenAt, applicationCloseAt, at(10)));
	}

	// 종목 11을 raceId 대회의 종목으로 넣는다
	private void saveCourse(int raceId, boolean deleted) {
		queueRaceCourseRepository.save(new QueueRaceCourse(RACE_COURSE_ID, raceId, at(10), deleted));
	}

	// QueueRedisRepository와 같은 방식으로 score(epoch ms)를 만든다
	private static double epochMillis(LocalDateTime time) {
		return time.atZone(ZONE).toInstant().toEpochMilli();
	}
}
