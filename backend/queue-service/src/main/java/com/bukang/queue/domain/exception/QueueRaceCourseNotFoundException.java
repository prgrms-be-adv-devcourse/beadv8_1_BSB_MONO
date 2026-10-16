package com.bukang.queue.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

/**
 * 대기열 복제본에 없는 종목으로 줄을 서려 할 때 (상품 API로 대신 조회하지 않는다)
 */
public class QueueRaceCourseNotFoundException extends BusinessException {
	public QueueRaceCourseNotFoundException() {
		super(HttpStatus.NOT_FOUND, "존재하지 않는 종목입니다.");
	}
}
