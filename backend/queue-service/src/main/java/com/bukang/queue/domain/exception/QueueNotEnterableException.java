package com.bukang.queue.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

/**
 * 지금 그 대회·종목의 줄을 설 수 없을 때
 * 다른 대회의 종목이거나 대회에서 뺀 종목, 또는 접수 기간이 아님(판매 중이 아님, 개인 예매 오픈 전, 접수 마감 뒤)
 */
public class QueueNotEnterableException extends BusinessException {
	public QueueNotEnterableException(String message) {
		super(HttpStatus.FORBIDDEN, message);
	}
}
