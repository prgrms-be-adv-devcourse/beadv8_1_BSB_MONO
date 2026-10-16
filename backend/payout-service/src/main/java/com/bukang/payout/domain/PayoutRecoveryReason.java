package com.bukang.payout.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 회수 사유 (payout.md 4-2). 회수 1건의 단위가 사유마다 다르다: 대회 취소는 대회당 1건
 * 일정 변경(환불 신청마다 1건)은 환불을 정산에 알려 주는 길(PRO-174)이 생기면 더한다
 */
@Getter
@RequiredArgsConstructor
public enum PayoutRecoveryReason {
	RACE_CANCELED("대회 취소");

	private final String korean;
}
