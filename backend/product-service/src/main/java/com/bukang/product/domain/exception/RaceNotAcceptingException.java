package com.bukang.product.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

/**
 * 개인 예매 접수 기간이 아니거나 판매 중이 아닌(판매 중지 등) 대회의 자리를 확보하려 할 때
 */
public class RaceNotAcceptingException extends BusinessException {
	public RaceNotAcceptingException() {
		super(HttpStatus.CONFLICT, "접수 기간이 아닙니다.");
	}
}
