package com.bukang.product.in;

/**
 * 대회 등록 중복 방지 키 헤더. order-service의 IdempotencyKeyHeader와 같은 이름·길이·메시지를 쓴다
 * 최대 길이는 Race.idempotencyKey 컬럼 길이(64)와 같다
 */
final class IdempotencyKeyHeader {
	static final String NAME = "Idempotency-Key";
	static final int MAX_LENGTH = 64;
	static final String INVALID_MESSAGE = "Idempotency-Key는 1~64자로 보내주세요.";

	private IdempotencyKeyHeader() {
	}
}
