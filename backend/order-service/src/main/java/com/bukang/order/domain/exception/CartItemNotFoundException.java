package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 장바구니에 없는 항목
public class CartItemNotFoundException extends BusinessException {
	public static final String CODE = "ORDER_CART_ITEM_NOT_FOUND";

	public CartItemNotFoundException(String message) {
		super(HttpStatus.NOT_FOUND, CODE, message);
	}
}
