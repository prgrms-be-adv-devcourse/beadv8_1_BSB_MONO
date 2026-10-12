package com.bukang.order.support;

import java.time.LocalDate;

import com.bukang.order.out.payout.RefundEligibilityPort;

/**
 * 테스트용 환불 가능 여부. 기본은 환불 가능이고, 테스트가 정산이 시작된 상황을 만들 수 있다
 */
public class StubRefundEligibilityPort implements RefundEligibilityPort {
	private volatile boolean refundable = true;

	@Override
	public boolean isRefundable(LocalDate raceDate) {
		return refundable;
	}

	public void setRefundable(boolean refundable) {
		this.refundable = refundable;
	}

	public void clear() {
		refundable = true;
	}
}
