package com.bukang.product.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

/**
 * 검색 문서(ES)가 돌려줄 수 있는 범위를 넘은 대회 목록 요청 (페이지 번호가 너무 큼)
 */
public class RaceSearchRangeException extends BusinessException {
	public RaceSearchRangeException(String message) {
		super(HttpStatus.BAD_REQUEST, message);
	}
}
