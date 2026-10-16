package com.bukang.payout.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 정산 항목 종류
 * 지금은 SALE만 생긴다: 환불 마감일까지 환불된 참가권은 빠지고, 1차 생성 뒤 환불은 아직 다루지 않는다.
 * REFUND는 1차 생성 뒤 · 지급 전 환불을 음수 금액으로 기록하기 위해 둔다(PRO-174). 지급 뒤 환불은 항목이 아니라 회수(PRO-33)다
 */
@Getter
@RequiredArgsConstructor
public enum PayoutRecordType {
	SALE("판매"),
	REFUND("환불");

	private final String korean;
}
