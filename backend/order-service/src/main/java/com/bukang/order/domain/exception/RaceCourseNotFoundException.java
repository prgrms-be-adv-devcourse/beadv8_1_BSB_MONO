package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 상품 서비스(B)에 없는 종목
public class RaceCourseNotFoundException extends BusinessException {
	public RaceCourseNotFoundException(String message) {
		super(HttpStatus.NOT_FOUND, message);
	}
}
