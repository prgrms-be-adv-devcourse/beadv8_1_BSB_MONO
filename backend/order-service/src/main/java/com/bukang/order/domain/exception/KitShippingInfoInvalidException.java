package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 키트 발송에 넣은 택배사·송장 번호가 비었거나 너무 김
public class KitShippingInfoInvalidException extends BusinessException {
	public static final String CODE = "ORDER_INVALID_KIT_SHIPPING_INFO";

	public KitShippingInfoInvalidException(String message) {
		super(HttpStatus.BAD_REQUEST, CODE, message);
	}
}
