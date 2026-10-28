package com.bukang.payout.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 판매자 정산 화면 위쪽 요약 카드 (payout.md 1장 조회 · 보관 4번)
 * 목록은 기간으로 잘리므로 목록에서 계산하지 않고, 기간과 상관없이 판매자의 지금 상태로 계산한다
 */
@AllArgsConstructor
@Getter
public class PayoutSummaryDto {
	// 다음 정산일: 지급 예정 정산 내역 중 가장 이른 지급 예정일. 1차를 기다리는 최종은 뺀다 (PRO-188)
	// 없으면 null (화면은 "-"로 보여 준다)
	private final LocalDate nextPayDate;
	// 정산 예정액: 다음 정산일에 나갈 지급액 합계 (보류된 정산 내역과 1차를 기다리는 최종 제외). 다음 정산일이 null이면 0
	private final long scheduledPayoutAmount;
	// 지급보류 중인 정산 내역 수
	private final long heldCount;
}
