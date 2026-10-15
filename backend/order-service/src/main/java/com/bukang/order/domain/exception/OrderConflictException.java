package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 같은 주문 요청이 동시에 들어와 유니크 제약에 걸림
public class OrderConflictException extends BusinessException {
	public static final String CODE = "ORDER_REQUEST_IN_PROGRESS";
	public static final String KEY_REUSED_CODE = "ORDER_IDEMPOTENCY_KEY_REUSED";

	public OrderConflictException(String message) {
		this(CODE, message);
	}

	private OrderConflictException(String code, String message) {
		super(HttpStatus.CONFLICT, code, message);
	}

	// 같은 Idempotency-Key를 다른 참가 신청에 다시 씀
	public static OrderConflictException keyReused(String message) {
		return new OrderConflictException(KEY_REUSED_CODE, message);
	}
}
