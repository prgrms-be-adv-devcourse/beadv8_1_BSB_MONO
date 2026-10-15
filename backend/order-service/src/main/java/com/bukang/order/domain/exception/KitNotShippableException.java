package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 키트를 발송 처리할 수 없음 (키트 없음, 결제 완료가 아님, 이미 발송함, 환불 기한 전)
public class KitNotShippableException extends BusinessException {
	public static final String CODE = "ORDER_KIT_NOT_SHIPPABLE";

	public KitNotShippableException(String message) {
		super(HttpStatus.CONFLICT, CODE, message);
	}
}
