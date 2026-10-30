package com.bukang.order.in;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.bukang.order.app.OrderOutboxKafkaAppendUseCase;
import com.bukang.order.domain.event.OrderExpiredEvent;

import lombok.RequiredArgsConstructor;

/**
 * 주문이 만료되면 상품 서비스에 결제하지 않은 자리를 풀어 달라고 알린다 (PRO-166)
 * 서비스 안은 스프링 이벤트, 서비스 사이는 Kafka로 주고받는다 (10/14 팀 약속)
 * 만료 트랜잭션 안에서 해제 메시지(product.hold.cancel)를 아웃박스에 저장하고, 커밋 뒤 릴레이가 보낸다 (PRO-270)
 * 만료가 롤백되면 메시지도 없고, 커밋되면 Kafka가 잠시 멈춰도 다시 보낸다. 상품의 해제는 멱등이다
 */
@Component
@RequiredArgsConstructor
public class OrderExpiredEventListener {
	private final OrderOutboxKafkaAppendUseCase orderOutboxKafkaAppendUseCase;

	@EventListener
	public void releaseSeat(OrderExpiredEvent event) {
		orderOutboxKafkaAppendUseCase.appendSeatRelease(event.participationId());
	}
}
