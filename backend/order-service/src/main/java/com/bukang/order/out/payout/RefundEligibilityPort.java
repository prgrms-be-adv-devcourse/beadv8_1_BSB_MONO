package com.bukang.order.out.payout;

import java.time.LocalDate;

/**
 * 정산 서비스(E)에 참가권을 환불할 수 있는지 묻는다 (GET /internal/v1/payout/refund-eligibility?raceId=&raceDate=)
 * 정산 서비스는 대회의 환불 마감일 다음 날(1차 정산 대상일)부터 사유와 상관없이 환불을 막는다 (PRO-32)
 * 환불 마감일을 모르면 대회 다음 날부터 막는다. 환불 기한 판단은 주문 서비스가 따로 한다
 */
public interface RefundEligibilityPort {
	boolean isRefundable(int raceId, LocalDate raceDate);
}
