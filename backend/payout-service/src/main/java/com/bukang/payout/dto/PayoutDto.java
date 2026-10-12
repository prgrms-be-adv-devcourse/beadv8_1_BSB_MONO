package com.bukang.payout.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.bukang.payout.domain.Payout;
import com.bukang.payout.domain.PayoutStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 판매자에게 보여 주는 정산 내역 (월별 정산 탭의 한 줄)
 * 대회 이름은 정산 내역을 만들 때 저장한 값이다 (정산한 때의 이름)
 */
@AllArgsConstructor
@Getter
public class PayoutDto {
	private final int id;
	private final String payoutCode;
	private final int raceId;
	private final String raceName;
	private final String payoutMonth;
	private final long salesAmount;
	private final long refundAmount;
	private final long feeAmount;
	private final long payoutAmount;
	private final PayoutStatus status;
	private final LocalDateTime settledAt;
	// 지급 예정일. 보류를 풀면 해제일 뒤 돌아오는 지급일로 바뀐다
	private final LocalDate scheduledPayDate;
	// 지급 완료 전에는 null
	private final LocalDateTime paidAt;

	public static PayoutDto from(Payout payout) {
		return new PayoutDto(
			payout.getId(),
			payout.getPayoutCode(),
			payout.getRaceId(),
			payout.getRaceName(),
			payout.getPayoutMonth(),
			payout.getSalesAmount(),
			payout.getRefundAmount(),
			payout.getFeeAmount(),
			payout.getPayoutAmount(),
			payout.getStatus(),
			payout.getSettledAt(),
			payout.getScheduledPayDate(),
			payout.getPaidAt()
		);
	}
}
