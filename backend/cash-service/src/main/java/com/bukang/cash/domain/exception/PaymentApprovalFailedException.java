package com.bukang.cash.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// PG(Toss페이먼츠)에 결제 승인을 요청했으나 실패함 (연결 실패, 응답 시간 초과, 5xx, 거절 응답)
public class PaymentApprovalFailedException extends BusinessException {
	public PaymentApprovalFailedException(String message) {
		super(HttpStatus.SERVICE_UNAVAILABLE, message);
	}
}
