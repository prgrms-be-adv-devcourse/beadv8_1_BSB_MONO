package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 접수 기간이 아니라 신청하거나 참가자 정보를 고칠 수 없음 (정책 3장 1-5)
public class ApplicationPeriodException extends BusinessException {
	public ApplicationPeriodException(String message) {
		super(HttpStatus.CONFLICT, message);
	}
}
