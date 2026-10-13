package com.bukang.product.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

/**
 * 대회의 지금 상태에서는 할 수 없는 변경 (판매 시작 뒤 참가비 인상, 정원을 확보·판매된 자리보다 줄이기 등)
 */
public class RaceChangeNotAllowedException extends BusinessException {
	public RaceChangeNotAllowedException(String message) {
		super(HttpStatus.CONFLICT, message);
	}
}
