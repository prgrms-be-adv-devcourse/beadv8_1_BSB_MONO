package com.bukang.payout.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

public class PayoutRecoveryNotFoundException extends BusinessException {
	public PayoutRecoveryNotFoundException() {
		super(HttpStatus.NOT_FOUND, "회수를 찾을 수 없습니다.");
	}
}
