package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 다른 서비스(상품·예치금·정산)에 연결하지 못함 (연결 실패, 응답 시간 초과, 5xx, 응답 형식 오류)
public class ExternalServiceUnavailableException extends BusinessException {
	public static final String CODE = "ORDER_EXTERNAL_SERVICE_UNAVAILABLE";

	public ExternalServiceUnavailableException(String message) {
		super(HttpStatus.SERVICE_UNAVAILABLE, CODE, message);
	}
}
