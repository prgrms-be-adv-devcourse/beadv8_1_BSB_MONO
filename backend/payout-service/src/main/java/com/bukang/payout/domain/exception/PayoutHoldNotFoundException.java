package com.bukang.payout.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

public class PayoutHoldNotFoundException extends BusinessException {
	public PayoutHoldNotFoundException() {
		super(HttpStatus.NOT_FOUND, "지급보류를 찾을 수 없습니다.");
	}
}
