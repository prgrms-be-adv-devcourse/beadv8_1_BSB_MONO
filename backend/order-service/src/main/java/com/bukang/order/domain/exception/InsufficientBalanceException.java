package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 예치금이 부족함. 주문과 자리는 그대로 두고 충전 뒤 같은 주문으로 다시 결제한다
public class InsufficientBalanceException extends BusinessException {
	public InsufficientBalanceException(String message) {
		super(HttpStatus.CONFLICT, message);
	}
}
