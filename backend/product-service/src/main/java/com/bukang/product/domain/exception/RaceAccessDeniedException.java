package com.bukang.product.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

/**
 * 다른 판매자가 등록한 대회를 수정하거나 취소하려 할 때
 */
public class RaceAccessDeniedException extends BusinessException {
	public RaceAccessDeniedException() {
		super(HttpStatus.FORBIDDEN, "본인이 등록한 대회만 처리할 수 있습니다.");
	}
}
