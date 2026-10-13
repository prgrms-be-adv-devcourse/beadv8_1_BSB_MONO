package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 취소할 수 없는 주문 상품 (결제 전, 이미 취소)
public class OrderItemNotCancelableException extends BusinessException {
	public OrderItemNotCancelableException(String message) {
		super(HttpStatus.CONFLICT, message);
	}
}
