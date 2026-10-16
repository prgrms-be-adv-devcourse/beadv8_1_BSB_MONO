package com.bukang.member.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 끝나지 않은 참가권이 있으면 탈퇴할 수 없다 (BE-022)
public class WithdrawTicketLeftException extends BusinessException {
	public WithdrawTicketLeftException(String message) {
		super(HttpStatus.CONFLICT, "MEMBER_WITHDRAW_TICKET_LEFT", message);
	}
}
