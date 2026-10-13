package com.bukang.product.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

/**
 * 없는 종목이나 아직 판매를 시작하지 않은(작성 중) 대회의 종목을 조회할 때
 */
public class RaceCourseNotFoundException extends BusinessException {
	public RaceCourseNotFoundException() {
		super(HttpStatus.NOT_FOUND, "존재하지 않는 종목입니다.");
	}
}
