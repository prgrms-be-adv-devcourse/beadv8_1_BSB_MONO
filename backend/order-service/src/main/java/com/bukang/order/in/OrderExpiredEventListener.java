package com.bukang.order.in;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.bukang.order.domain.event.OrderExpiredEvent;
import com.bukang.order.out.seat.SeatHoldPort;

import lombok.RequiredArgsConstructor;

/**
 * 주문이 만료되면 상품 서비스에 결제하지 않은 자리를 풀어 달라고 알린다 (PRO-166)
 * 서비스 안은 스프링 이벤트, 서비스 사이는 Kafka로 주고받는다 (10/14 팀 약속)
 * 커밋 뒤가 아니라 만료 트랜잭션 안에서 보내고 결과를 기다린다. 보내지 못하면 예외로 만료가 롤백되어 다음 실행에서 다시 보낸다
 * (커밋 뒤에 보내다 실패하면 이미 만료된 주문이라 다시 보낼 기회가 없고, 상품 자리가 영원히 묶인다)
 * 보낸 뒤 커밋이 실패해도 해가 없다: 상품의 해제는 멱등이고, 이 주문은 기한이 지나 결제할 수 없으며 다음 실행에서 다시 만료된다
 */
@Component
@RequiredArgsConstructor
public class OrderExpiredEventListener {
	private final SeatHoldPort seatHoldPort;

	@EventListener
	public void releaseSeat(OrderExpiredEvent event) {
		seatHoldPort.release(event.participationId());
	}
}
