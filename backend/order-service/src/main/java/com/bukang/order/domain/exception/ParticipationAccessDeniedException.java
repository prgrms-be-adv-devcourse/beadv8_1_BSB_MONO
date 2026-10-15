package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 다른 회원의 참가 신청
public class ParticipationAccessDeniedException extends BusinessException {
	public static final String CODE = "ORDER_PARTICIPATION_FORBIDDEN";

	public ParticipationAccessDeniedException(String message) {
		super(HttpStatus.FORBIDDEN, CODE, message);
	}
}
