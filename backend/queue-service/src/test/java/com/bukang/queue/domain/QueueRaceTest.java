package com.bukang.queue.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bukang.common.shared.race.domain.RaceSaleStatus;

/**
 * 대기열 진입 판정(isEnterable)의 시각 경계를 지금 시각을 직접 넣어 확인한다.
 * 개인 예매 오픈 시각은 포함하고(이상), 접수 마감 시각은 포함하지 않는다(미만).
 */
class QueueRaceTest {
	private static final LocalDateTime INDIVIDUAL_OPEN_AT = LocalDateTime.of(2026, 10, 20, 10, 0);
	private static final LocalDateTime APPLICATION_CLOSE_AT = LocalDateTime.of(2026, 11, 1, 18, 0);

	private final QueueRace race = new QueueRace(1, RaceSaleStatus.ON_SALE, INDIVIDUAL_OPEN_AT, APPLICATION_CLOSE_AT,
		LocalDateTime.of(2026, 10, 15, 10, 0));

	@Test
	@DisplayName("isEnterable: 개인 예매 오픈 시각 정각부터 들어올 수 있다 (1나노초 전은 안 된다)")
	void enterableFromIndividualOpenAt() {
		assertThat(race.isEnterable(INDIVIDUAL_OPEN_AT)).isTrue();
		assertThat(race.isEnterable(INDIVIDUAL_OPEN_AT.minusNanos(1))).isFalse();
	}

	@Test
	@DisplayName("isEnterable: 접수 마감 시각 정각부터는 들어올 수 없다 (1나노초 전까지는 된다)")
	void notEnterableFromApplicationCloseAt() {
		assertThat(race.isEnterable(APPLICATION_CLOSE_AT)).isFalse();
		assertThat(race.isEnterable(APPLICATION_CLOSE_AT.minusNanos(1))).isTrue();
	}
}
