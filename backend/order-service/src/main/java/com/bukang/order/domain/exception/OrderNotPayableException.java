package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 결제할 수 없는 주문 (결제 대기 아님, 기한 지남, 판매 실패)
public class OrderNotPayableException extends BusinessException {
	public OrderNotPayableException(String message) {
		super(HttpStatus.CONFLICT, message);
	}
}
