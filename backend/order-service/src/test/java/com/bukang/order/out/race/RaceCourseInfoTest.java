package com.bukang.order.out.race;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RaceCourseInfoTest {
	private static final LocalDateTime RACE_DATE = LocalDateTime.of(2026, 11, 15, 8, 0);
	private static final LocalDateTime OPEN_AT = LocalDateTime.of(2026, 10, 1, 10, 0);
	private static final LocalDateTime CLOSE_AT = LocalDateTime.of(2026, 10, 31, 23, 59);

	@Test
	@DisplayName("나이는 대회 당일의 만 나이로 따진다. 대회 날이 생일이면 그날 한 살 많다")
	void ageOnRaceDay() {
		RaceCourseInfo course = course(14, null);

		assertThat(course.ageOnRaceDay(LocalDate.of(2012, 11, 15))).isEqualTo(14);
		assertThat(course.ageOnRaceDay(LocalDate.of(2012, 11, 16))).isEqualTo(13);
	}

	@Test
	@DisplayName("최소 나이와 최대 나이 경계값을 포함한다. 최대 나이가 없으면 위로 제한이 없다")
	void allowsAge() {
		RaceCourseInfo limited = course(20, 60);
		assertThat(limited.allowsAge(LocalDate.of(2006, 11, 15))).isTrue();
		assertThat(limited.allowsAge(LocalDate.of(2006, 11, 16))).isFalse();
		assertThat(limited.allowsAge(LocalDate.of(1966, 11, 16))).isTrue();
		assertThat(limited.allowsAge(LocalDate.of(1965, 11, 15))).isFalse();

		RaceCourseInfo noMax = course(14, null);
		assertThat(noMax.allowsAge(LocalDate.of(1930, 1, 1))).isTrue();
		assertThat(noMax.ageRangeLabel()).isEqualTo("만 14세 이상");
		assertThat(limited.ageRangeLabel()).isEqualTo("만 20세 ~ 만 60세");
	}

	@Test
	@DisplayName("접수 기간은 오픈 시각부터 마감 시각 전까지다")
	void accepting() {
		RaceCourseInfo course = course(14, null);

		assertThat(course.isAccepting(OPEN_AT.minusSeconds(1))).isFalse();
		assertThat(course.isAccepting(OPEN_AT)).isTrue();
		assertThat(course.isAccepting(CLOSE_AT.minusSeconds(1))).isTrue();
		assertThat(course.isAccepting(CLOSE_AT)).isFalse();
		assertThat(course.isBeforeClose(OPEN_AT.minusDays(1))).isTrue();
		assertThat(course.isBeforeClose(CLOSE_AT)).isFalse();
	}

	private RaceCourseInfo course(int minAge, Integer maxAge) {
		return new RaceCourseInfo(1, 1, RACE_DATE, OPEN_AT, CLOSE_AT, minAge, maxAge);
	}
}
