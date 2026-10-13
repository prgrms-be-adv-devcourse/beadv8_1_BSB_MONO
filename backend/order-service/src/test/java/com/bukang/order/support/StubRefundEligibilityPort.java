package com.bukang.order.support;

import java.time.LocalDate;

import com.bukang.order.out.payout.RefundEligibilityPort;

/**
 * 테스트용 환불 가능 여부. 기본은 환불 가능이고, 테스트가 정산이 시작된 상황을 만들 수 있다
 */
public class StubRefundEligibilityPort implements RefundEligibilityPort {
	private volatile boolean refundable = true;
	private volatile LocalDate lastRaceDate;

	@Override
	public boolean isRefundable(LocalDate raceDate) {
		lastRaceDate = raceDate;
		return refundable;
	}

	// 마지막으로 물어본 대회 날짜 (어느 날짜로 정산 대상일을 따졌는지 확인할 때 쓴다)
	public LocalDate lastRaceDate() {
		return lastRaceDate;
	}

	public void setRefundable(boolean refundable) {
		this.refundable = refundable;
	}

	public void clear() {
		refundable = true;
		lastRaceDate = null;
	}
}
