package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 다른 회원의 주문
public class OrderAccessDeniedException extends BusinessException {
	public static final String CODE = "ORDER_FORBIDDEN";

	public OrderAccessDeniedException(String message) {
		super(HttpStatus.FORBIDDEN, CODE, message);
	}
}
