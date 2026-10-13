package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 결제 전(PENDING)이 아닌 참가 신청서를 신청서 취소로 버리려 함
public class ParticipationNotCancelableException extends BusinessException {
	public ParticipationNotCancelableException(String message) {
		super(HttpStatus.CONFLICT, message);
	}
}
