package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 장바구니에 이미 다른 종목이 담겨 있음 (장바구니에는 한 종목만 담는다, PRO-192)
public class CartCourseLimitException extends BusinessException {
	public static final String CODE = "ORDER_CART_ONE_COURSE_ONLY";

	public CartCourseLimitException(String message) {
		super(HttpStatus.CONFLICT, CODE, message);
	}
}
