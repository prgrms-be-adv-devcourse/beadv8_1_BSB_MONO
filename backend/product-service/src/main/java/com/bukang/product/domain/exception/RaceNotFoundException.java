package com.bukang.product.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

/**
 * 없는 대회를 수정하거나 판매를 시작하려 할 때
 */
public class RaceNotFoundException extends BusinessException {
	public RaceNotFoundException() {
		super(HttpStatus.NOT_FOUND, "존재하지 않는 대회입니다.");
	}
}
