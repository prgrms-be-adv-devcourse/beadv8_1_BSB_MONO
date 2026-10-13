package com.bukang.product.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

/**
 * 값이 맞지 않는 자리 확보 요청 (끝 시각이 이미 지남 등)
 */
public class InvalidHoldException extends BusinessException {
	public InvalidHoldException(String message) {
		super(HttpStatus.BAD_REQUEST, message);
	}
}
