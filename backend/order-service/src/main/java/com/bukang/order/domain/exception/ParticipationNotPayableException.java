package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 자리 확보 전(PENDING)이 아닌 참가 신청으로 [결제하기]를 누름
public class ParticipationNotPayableException extends BusinessException {
	public static final String CODE = "ORDER_PARTICIPATION_NOT_PAYABLE";

	public ParticipationNotPayableException(String message) {
		super(HttpStatus.CONFLICT, CODE, message);
	}
}
