package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 대회 당일 만 나이가 종목의 나이 제한 밖 (정책 2장 1-9)
public class AgeRestrictionException extends BusinessException {
	public AgeRestrictionException(String message) {
		super(HttpStatus.BAD_REQUEST, message);
	}
}
