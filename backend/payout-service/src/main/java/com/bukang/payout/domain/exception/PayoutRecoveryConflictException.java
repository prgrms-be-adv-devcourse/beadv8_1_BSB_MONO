package com.bukang.payout.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

/**
 * 지금 상태로는 회수를 처리할 수 없을 때 (이미 완료됨)
 */
public class PayoutRecoveryConflictException extends BusinessException {
	public PayoutRecoveryConflictException(String message) {
		super(HttpStatus.CONFLICT, message);
	}
}
