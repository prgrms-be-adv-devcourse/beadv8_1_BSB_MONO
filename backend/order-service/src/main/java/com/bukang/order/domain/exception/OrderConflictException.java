package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 같은 주문 요청이 동시에 들어와 유니크 제약에 걸림
public class OrderConflictException extends BusinessException {
	public OrderConflictException(String message) {
		super(HttpStatus.CONFLICT, message);
	}
}
