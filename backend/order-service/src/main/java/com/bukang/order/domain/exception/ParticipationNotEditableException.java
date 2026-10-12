package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 취소·해제·확보 실패한 신청이라 고칠 수 없음
public class ParticipationNotEditableException extends BusinessException {
	public ParticipationNotEditableException(String message) {
		super(HttpStatus.CONFLICT, message);
	}
}
