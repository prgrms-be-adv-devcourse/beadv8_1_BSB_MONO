package com.bukang.payout.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

/**
 * 지금 상태로는 지급보류를 걸거나 풀 수 없을 때 (이미 보류 중, 이미 지급 완료, 이미 해제됨)
 */
public class PayoutHoldConflictException extends BusinessException {
	public PayoutHoldConflictException(String message) {
		super(HttpStatus.CONFLICT, message);
	}
}
