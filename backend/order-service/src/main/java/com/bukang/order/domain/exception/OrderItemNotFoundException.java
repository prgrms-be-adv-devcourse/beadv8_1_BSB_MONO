package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 주문에 없는 주문 상품
public class OrderItemNotFoundException extends BusinessException {
	public OrderItemNotFoundException(String message) {
		super(HttpStatus.NOT_FOUND, message);
	}
}
