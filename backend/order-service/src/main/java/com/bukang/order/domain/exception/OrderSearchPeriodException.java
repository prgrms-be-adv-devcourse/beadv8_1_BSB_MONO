package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 주문 조회 기간이 잘못됨 (시작일이 종료일보다 늦음)
public class OrderSearchPeriodException extends BusinessException {
	public static final String CODE = "ORDER_INVALID_SEARCH_PERIOD";

	public OrderSearchPeriodException(String message) {
		super(HttpStatus.BAD_REQUEST, CODE, message);
	}
}
