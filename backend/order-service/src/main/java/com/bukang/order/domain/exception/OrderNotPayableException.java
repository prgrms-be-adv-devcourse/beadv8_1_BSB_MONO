package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 결제할 수 없는 주문 (결제 대기 아님, 기한 지남, 판매 실패)
public class OrderNotPayableException extends BusinessException {
	public static final String CODE = "ORDER_NOT_PAYABLE";
	public static final String DEADLINE_PASSED_CODE = "ORDER_PAYMENT_DEADLINE_PASSED";

	public OrderNotPayableException(String message) {
		this(CODE, message);
	}

	private OrderNotPayableException(String code, String message) {
		super(HttpStatus.CONFLICT, code, message);
	}

	// 결제 기한이 지남 (주문 사본 또는 상품 판매 거절)
	public static OrderNotPayableException deadlinePassed(String message) {
		return new OrderNotPayableException(DEADLINE_PASSED_CODE, message);
	}
}
