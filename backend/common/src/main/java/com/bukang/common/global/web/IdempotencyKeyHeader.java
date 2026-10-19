package com.bukang.common.global.web;

/**
 * 같은 요청을 두 번 처리하지 않도록 클라이언트가 보내는 중복 요청 방지 키 헤더.
 * 이 키를 받는 API가 같은 이름·길이·메시지를 쓰도록 모든 서비스가 이 상수를 쓴다.
 * 최대 길이는 각 서비스에서 키를 저장하는 컬럼 길이(64)와 같다
 */
public final class IdempotencyKeyHeader {
	public static final String NAME = "Idempotency-Key";
	public static final int MAX_LENGTH = 64;
	public static final String INVALID_MESSAGE = "Idempotency-Key는 1~64자로 보내주세요.";

	private IdempotencyKeyHeader() {
	}
}
