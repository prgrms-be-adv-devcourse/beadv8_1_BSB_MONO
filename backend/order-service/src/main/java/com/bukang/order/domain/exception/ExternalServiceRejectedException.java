package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

/**
 * 다른 서비스가 요청을 4xx로 거절함 (경로·요청 계약이 어긋남, 지갑 없음 등). 다시 해도 같은 결과라 장애(ExternalServiceUnavailableException)와 나눈다
 * 회원에게는 장애와 같은 503으로 알린다. 대회 취소 환불은 이것을 "그 주문만 실패"로 세고 다음 주문을 계속 환불한다 (#96 리뷰)
 */
public class ExternalServiceRejectedException extends BusinessException {
	// 회원·프론트에는 장애와 같은 응답으로 보인다 (#100의 ExternalServiceUnavailableException.CODE와 같은 값)
	public static final String CODE = "ORDER_EXTERNAL_SERVICE_UNAVAILABLE";

	public ExternalServiceRejectedException(String message) {
		super(HttpStatus.SERVICE_UNAVAILABLE, CODE, message);
	}
}
