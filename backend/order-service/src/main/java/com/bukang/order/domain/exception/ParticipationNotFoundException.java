package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 없는 참가 신청
public class ParticipationNotFoundException extends BusinessException {
	public ParticipationNotFoundException(String message) {
		super(HttpStatus.NOT_FOUND, message);
	}
}
