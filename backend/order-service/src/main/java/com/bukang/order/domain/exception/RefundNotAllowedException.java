package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 환불할 수 없는 참가권 (환불 기한 지남, 정산 대상일 지남)
public class RefundNotAllowedException extends BusinessException {
	public RefundNotAllowedException(String message) {
		super(HttpStatus.CONFLICT, message);
	}
}
