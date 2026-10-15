package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 접수 기간이 아니라 신청하거나 참가자 정보를 고칠 수 없음 (정책 3장 1-5)
public class ApplicationPeriodException extends BusinessException {
	public static final String CODE = "ORDER_APPLICATION_CLOSED";
	public static final String RACE_CANCELED_CODE = "ORDER_RACE_CANCELED";

	public ApplicationPeriodException(String message) {
		this(CODE, message);
	}

	private ApplicationPeriodException(String code, String message) {
		super(HttpStatus.CONFLICT, code, message);
	}

	// 판매자가 취소한 대회
	public static ApplicationPeriodException raceCanceled(String message) {
		return new ApplicationPeriodException(RACE_CANCELED_CODE, message);
	}
}
