package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 장바구니 결제 요청이 올바르지 않음 (같은 참가 신청을 두 번 보냄)
public class CartCheckoutInvalidException extends BusinessException {
	public static final String CODE = "ORDER_CART_CHECKOUT_INVALID";

	public CartCheckoutInvalidException(String message) {
		super(HttpStatus.BAD_REQUEST, CODE, message);
	}
}
