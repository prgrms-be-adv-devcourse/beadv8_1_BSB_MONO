package com.bukang.member.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 예치금·참가권 확인에 실패하면 탈퇴하지 않는다. 막는 쪽으로 실패한다 (BE-022)
public class WithdrawCheckFailedException extends BusinessException {
	public WithdrawCheckFailedException(String message) {
		super(HttpStatus.SERVICE_UNAVAILABLE, "MEMBER_WITHDRAW_CHECK_FAILED", message);
	}
}
