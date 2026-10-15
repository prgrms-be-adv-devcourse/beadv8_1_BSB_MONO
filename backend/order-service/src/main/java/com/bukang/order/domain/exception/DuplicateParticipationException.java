package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 같은 대회에 이미 살아 있는 신청이 있음 (한 사람 한 대회 1건, 정책 1장 3-1)
public class DuplicateParticipationException extends BusinessException {
	public static final String CODE = "ORDER_DUPLICATE_PARTICIPATION";

	public DuplicateParticipationException(String message) {
		super(HttpStatus.CONFLICT, CODE, message);
	}
}
