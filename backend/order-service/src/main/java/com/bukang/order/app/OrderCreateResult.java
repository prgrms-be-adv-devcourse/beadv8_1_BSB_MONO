package com.bukang.order.app;

import com.bukang.order.dto.OrderDto;

/**
 * [결제하기] 결과. 매진은 예외가 아니라 결과로 돌려준다
 * (예외로 던지면 트랜잭션이 롤백되어 참가 신청을 FAILED로 바꾼 것까지 사라진다)
 */
public record OrderCreateResult(Outcome outcome, OrderDto order) {
	public enum Outcome {
		CREATED,
		// 같은 Idempotency-Key로 이미 만든 주문을 돌려줌
		EXISTING,
		SOLD_OUT
	}

	static OrderCreateResult created(OrderDto order) {
		return new OrderCreateResult(Outcome.CREATED, order);
	}

	static OrderCreateResult existing(OrderDto order) {
		return new OrderCreateResult(Outcome.EXISTING, order);
	}

	static OrderCreateResult soldOut() {
		return new OrderCreateResult(Outcome.SOLD_OUT, null);
	}
}
