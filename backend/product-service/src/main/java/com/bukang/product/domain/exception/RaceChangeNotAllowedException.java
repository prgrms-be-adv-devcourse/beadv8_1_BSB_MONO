package com.bukang.product.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

/**
 * 대회의 지금 상태에서는 할 수 없는 변경 (판매 시작 뒤 참가비·키트 금액 변경, 정원 줄이기 등)
 */
public class RaceChangeNotAllowedException extends BusinessException {
	public RaceChangeNotAllowedException(String message) {
		super(HttpStatus.CONFLICT, message);
	}
}
