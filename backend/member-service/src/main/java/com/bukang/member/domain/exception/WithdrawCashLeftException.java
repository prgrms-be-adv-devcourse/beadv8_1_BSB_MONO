package com.bukang.member.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 남은 예치금이 있으면 탈퇴할 수 없다 (BE-022)
public class WithdrawCashLeftException extends BusinessException {
	public WithdrawCashLeftException(String message) {
		super(HttpStatus.CONFLICT, "MEMBER_WITHDRAW_CASH_LEFT", message);
	}
}
