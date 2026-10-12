package com.bukang.payout.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bukang.payout.domain.exception.PayoutQueryException;

class PayoutQueryPeriodTest {

	@Test
	@DisplayName("정산 내역은 시작 달을 포함해 12개 달까지 조회할 수 있다 (2025-11 ~ 2026-10)")
	void payoutMonthsUpToTwelve() {
		assertThatCode(() -> PayoutQueryPeriod.checkPayoutMonths(YearMonth.of(2025, 11), YearMonth.of(2026, 10)))
			.doesNotThrowAnyException();
		assertThatCode(() -> PayoutQueryPeriod.checkPayoutMonths(YearMonth.of(2026, 9), YearMonth.of(2026, 9)))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("정산 내역을 13개 달 조회하면 막는다 (2025-11 ~ 2026-11)")
	void payoutMonthsOverTwelve() {
		assertThatThrownBy(() -> PayoutQueryPeriod.checkPayoutMonths(YearMonth.of(2025, 11), YearMonth.of(2026, 11)))
			.isInstanceOf(PayoutQueryException.class)
			.hasMessage("정산 내역은 최대 12개월까지 조회할 수 있습니다.");
	}

	@Test
	@DisplayName("건별 정산 · 지급보류는 끝 날이 시작 날 + 1개월 전까지 된다 (9/1 ~ 9/30, 9/15 ~ 10/14)")
	void detailDaysUpToOneMonth() {
		assertThatCode(() -> PayoutQueryPeriod.checkDetailDays(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
			.doesNotThrowAnyException();
		assertThatCode(() -> PayoutQueryPeriod.checkDetailDays(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 10, 14)))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("건별 정산 · 지급보류를 시작 날 + 1개월 당일까지 조회하면 막는다 (9/15 ~ 10/15)")
	void detailDaysOverOneMonth() {
		assertThatThrownBy(
			() -> PayoutQueryPeriod.checkDetailDays(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 10, 15)))
			.isInstanceOf(PayoutQueryException.class)
			.hasMessage("건별 정산·지급보류 내역은 최대 1개월까지 조회할 수 있습니다.");
	}

	@Test
	@DisplayName("시작이 끝보다 늦으면 막는다")
	void reversedPeriod() {
		assertThatThrownBy(() -> PayoutQueryPeriod.checkPayoutMonths(YearMonth.of(2026, 10), YearMonth.of(2026, 9)))
			.isInstanceOf(PayoutQueryException.class)
			.hasMessage("조회 시작이 끝보다 늦습니다.");
		assertThatThrownBy(
			() -> PayoutQueryPeriod.checkDetailDays(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 1)))
			.isInstanceOf(PayoutQueryException.class)
			.hasMessage("조회 시작이 끝보다 늦습니다.");
	}
}
