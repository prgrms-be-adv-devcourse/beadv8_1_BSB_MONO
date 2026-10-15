package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 같은 장바구니에 동시에 담는 요청이 겹침
public class CartConflictException extends BusinessException {
	public static final String CODE = "ORDER_CART_CONFLICT";

	public CartConflictException(String message) {
		super(HttpStatus.CONFLICT, CODE, message);
	}
}
