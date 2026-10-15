package com.bukang.payout.domain;

import java.time.LocalDate;

/**
 * 정산 대상일 (피그잼 06 대상일 1번, payout.md 4-3)
 * 1차: 환불 마감일 다음 날. 예: 마감 9/30 → 1차 대상일 10/1, 정산 월 2026-09 → 지급 10/20
 * 최종: 대회 다음 날. 예: 대회 9/25 → 최종 대상일 9/26, 정산 월 2026-09 → 지급 10/20
 * 정산 월은 대상일이 아니라 환불 마감일 · 대회일의 달이다 (월말이면 대상일은 다음 달이 된다)
 */
public final class PayoutTargetDate {
	// 대회 다음 날 (이의 제기 기간 없음. payout.md의 '+3일'은 10/8 제안이라 피그잼 06에 맞춰 고쳤다)
	private static final int DAYS_AFTER_RACE = 1;
	// 환불 마감일 다음 날
	private static final int DAYS_AFTER_REFUND_DEADLINE = 1;

	private PayoutTargetDate() {
	}

	public static LocalDate of(LocalDate raceDate) {
		return raceDate.plusDays(DAYS_AFTER_RACE);
	}

	// 정산 대상일이 targetDate인 대회의 대회일 (배치가 오늘 정산할 대회를 찾을 때 쓴다)
	public static LocalDate raceDateFor(LocalDate targetDate) {
		return targetDate.minusDays(DAYS_AFTER_RACE);
	}

	// 1차 정산 대상일: 환불 마감일 다음 날
	public static LocalDate firstOf(LocalDate refundDeadline) {
		return refundDeadline.plusDays(DAYS_AFTER_REFUND_DEADLINE);
	}

	// 1차 정산 대상일이 targetDate인 대회의 환불 마감일 (1차 배치가 오늘 정산할 대회를 찾을 때 쓴다)
	public static LocalDate refundDeadlineFor(LocalDate targetDate) {
		return targetDate.minusDays(DAYS_AFTER_REFUND_DEADLINE);
	}

	// 환불 마감일을 모를 때의 기준: 최종 정산 대상일(대회 다음 날) 전날까지만 환불할 수 있다 (PRO-28)
	// 정산 내역에 들어간 참가권이 환불되면 판매자에게 그대로 지급되므로 막는다. 환불 마감일을 알면 isRefundableUntil을 쓴다
	// 예: 대회 9/25 → 9/25까지 환불 가능, 9/26(정산 대상일)부터 불가
	public static boolean isRefundable(LocalDate raceDate, LocalDate today) {
		return today.isBefore(of(raceDate));
	}

	// 환불 마감일이 지나면 환불할 수 없다. 마감일 당일(23:59:59)까지 가능하고, 1차 정산이 생기는 다음 날부터 막는다 (PRO-32)
	// 1차 정산은 그날 살아 있던 참가권으로 만들고, 1차 생성 뒤 환불(음수 항목 · 회수)은 아직 다루지 않기 때문이다 (PRO-174)
	// 사유와 상관없이(일정 변경 · 대회 취소 전액 환불 포함) 막는다. 그 경우를 열어 줄지는 PRO-174에서 정한다
	// 예: 마감 9/30 → 9/30까지 환불 가능, 10/1(1차 정산 대상일)부터 불가
	public static boolean isRefundableUntil(LocalDate refundDeadline, LocalDate today) {
		return today.isBefore(firstOf(refundDeadline));
	}
}
