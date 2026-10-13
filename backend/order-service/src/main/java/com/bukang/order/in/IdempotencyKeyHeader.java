package com.bukang.order.in;

/**
 * 중복 주문 방지 키 헤더. [결제하기]와 장바구니 결제가 같은 이름·길이·메시지를 쓰도록 모아 둔다
 * 최대 길이는 Order.idempotencyKey 컬럼 길이(64)와 같다
 */
final class IdempotencyKeyHeader {
	static final String NAME = "Idempotency-Key";
	static final int MAX_LENGTH = 64;
	static final String INVALID_MESSAGE = "Idempotency-Key는 1~64자로 보내주세요.";

	private IdempotencyKeyHeader() {
	}
}
