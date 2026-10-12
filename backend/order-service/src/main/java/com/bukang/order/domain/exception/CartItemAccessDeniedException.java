package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 다른 회원의 장바구니 항목
public class CartItemAccessDeniedException extends BusinessException {
	public CartItemAccessDeniedException(String message) {
		super(HttpStatus.FORBIDDEN, message);
	}
}
