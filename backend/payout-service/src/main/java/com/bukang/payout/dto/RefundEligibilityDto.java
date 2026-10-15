package com.bukang.payout.dto;

import java.time.LocalDate;
import java.util.Optional;

import com.bukang.payout.domain.PayoutTargetDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 참가권 환불 가능 여부. 주문 서비스가 환불 요청을 받을 때 물어본다 (PRO-28)
 * 판정 기준(정산 대상일)을 함께 돌려줘서, 거절할 때 언제부터 막혔는지 안내할 수 있게 한다
 */
@AllArgsConstructor
@Getter
public class RefundEligibilityDto {
	private final LocalDate raceDate;
	// 대회의 환불 마감일. 상품 서비스에서 찾지 못하면 null이다
	private final LocalDate refundDeadline;
	// 이날부터 환불할 수 없다: 환불 마감일을 알면 1차 정산 대상일(마감일 다음 날), 모르면 대회 다음 날
	private final LocalDate payoutTargetDate;
	private final boolean refundable;

	// 환불 마감일을 알면 마감일 기준으로, 모르면 대회일 기준으로 판단한다 (PRO-32)
	public static RefundEligibilityDto of(LocalDate raceDate, Optional<LocalDate> refundDeadline, LocalDate today) {
		return refundDeadline
			.map(deadline -> new RefundEligibilityDto(raceDate, deadline, PayoutTargetDate.firstOf(deadline),
				PayoutTargetDate.isRefundableUntil(deadline, today)))
			.orElseGet(() -> new RefundEligibilityDto(raceDate, null, PayoutTargetDate.of(raceDate),
				PayoutTargetDate.isRefundable(raceDate, today)));
	}
}
