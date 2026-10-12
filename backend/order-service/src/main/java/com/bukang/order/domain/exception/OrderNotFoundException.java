package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 없는 주문
public class OrderNotFoundException extends BusinessException {
	public OrderNotFoundException(String message) {
		super(HttpStatus.NOT_FOUND, message);
	}
}
