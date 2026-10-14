package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 티셔츠를 주는 종목인데 사이즈를 고르지 않았거나, 종목이 주지 않는 사이즈를 골랐음
public class TshirtSizeNotAvailableException extends BusinessException {
	public TshirtSizeNotAvailableException(String message) {
		super(HttpStatus.BAD_REQUEST, message);
	}
}
