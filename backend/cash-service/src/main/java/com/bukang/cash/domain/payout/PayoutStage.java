package com.bukang.cash.domain.payout;

/**
 * 정산 단계 (payout-service의 PayoutStage와 값만 맞춘 cash-service 자체 enum, 서비스끼리는 의존하지 않는다)
 * FIRST(1차)는 수수료 없이 대금만 옮기고, FINAL(최종)은 대금과 수수료를 함께 옮긴다 (PRO-256)
 */
public enum PayoutStage {
	FIRST,
	FINAL
}
