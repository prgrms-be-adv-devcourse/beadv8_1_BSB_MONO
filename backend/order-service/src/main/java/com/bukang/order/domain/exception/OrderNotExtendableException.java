package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 결제 기한을 늘릴 수 없는 주문 (결제 대기 아님, 이미 늘림, 기한 지남)
public class OrderNotExtendableException extends BusinessException {
	public OrderNotExtendableException(String message) {
		super(HttpStatus.CONFLICT, message);
	}
}
