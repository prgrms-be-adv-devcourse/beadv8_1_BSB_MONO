package com.bukang.product.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

/**
 * 요청 값끼리 맞지 않는 대회 등록·수정 요청 (날짜 순서, 나이·인원 범위, 이미지 용도·개수 등)
 * 값 하나만 보는 검사는 요청 DTO의 검증 애노테이션이 한다.
 */
public class InvalidRaceException extends BusinessException {
	public InvalidRaceException(String message) {
		super(HttpStatus.BAD_REQUEST, message);
	}
}
