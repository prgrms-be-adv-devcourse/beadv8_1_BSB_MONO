package com.bukang.payout.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

public class PayoutQueryException extends BusinessException {
	public PayoutQueryException(String message) {
		super(HttpStatus.BAD_REQUEST, message);
	}
}
