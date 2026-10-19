package com.bukang.queue.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

/**
 * 대기열 복제본에 없는 대회로 줄을 서려 할 때 (상품 API로 대신 조회하지 않는다)
 */
public class QueueRaceNotFoundException extends BusinessException {
	public QueueRaceNotFoundException() {
		super(HttpStatus.NOT_FOUND, "존재하지 않는 대회입니다.");
	}
}
