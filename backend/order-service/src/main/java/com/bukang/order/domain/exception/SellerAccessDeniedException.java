package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 판매자 API를 승인된 판매자가 아닌 회원이 부름 (PRO-197)
public class SellerAccessDeniedException extends BusinessException {
	public static final String CODE = "ORDER_SELLER_FORBIDDEN";

	public SellerAccessDeniedException(String message) {
		super(HttpStatus.FORBIDDEN, CODE, message);
	}
}
