package com.bukang.queue.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

/**
 * Redis(대기열 줄)에 연결하지 못했을 때
 * 대기열 없이 통과시키면 주문·상품에 요청이 한꺼번에 몰리므로, 막고 잠시 뒤 다시 시도하게 한다 (예매 중단)
 */
public class QueueUnavailableException extends BusinessException {
	public QueueUnavailableException() {
		super(HttpStatus.SERVICE_UNAVAILABLE, "대기열에 연결하지 못했습니다. 잠시 뒤 다시 시도해주세요.");
	}
}
