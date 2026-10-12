package com.bukang.payout.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

public class PayoutNotFoundException extends BusinessException {
	public PayoutNotFoundException() {
		super(HttpStatus.NOT_FOUND, "정산 내역을 찾을 수 없습니다.");
	}
}
