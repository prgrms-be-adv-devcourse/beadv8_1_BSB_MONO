package com.bukang.order.in;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bukang.order.domain.event.OrderRaceCanceledEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * 대회 취소로 환불한 구매자에게 알린다 (PRO-135). 환불이 커밋된 뒤에만 받아, 롤백된 환불을 알리지 않는다
 * 아직 알림 기능(이메일·푸시·알림함)이 없어 로그만 남긴다. 알림 기능이 생기면 여기서 보낸다
 */
@Slf4j
@Component
public class RaceCanceledNotificationListener {
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void notifyBuyer(OrderRaceCanceledEvent event) {
		log.info("대회 취소로 참가권을 환불해 구매자에게 알립니다 (알림 기능 전이라 로그만). "
				+ "buyerId={}, orderId={}, raceId={}, refundedAmount={}",
			event.buyerId(), event.orderId(), event.raceId(), event.refundedAmount());
	}
}
