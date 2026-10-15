package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 대기열에 입장 중이 아니라 [결제하기]를 할 수 없음 (줄 서는 중, 입장 시간이 끝남, 대기열을 거치지 않음)
public class QueueNotAdmittedException extends BusinessException {
	public static final String CODE = "ORDER_QUEUE_NOT_ADMITTED";

	public QueueNotAdmittedException(String message) {
		super(HttpStatus.FORBIDDEN, CODE, message);
	}
}
