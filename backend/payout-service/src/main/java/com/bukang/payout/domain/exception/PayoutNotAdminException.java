package com.bukang.payout.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 관리자 API(지급보류 걸기·풀기, 회수 완료)를 일반 회원이 불렀다 (PRO-265)
public class PayoutNotAdminException extends BusinessException {
	public PayoutNotAdminException() {
		super(HttpStatus.FORBIDDEN, "PAYOUT_NOT_ADMIN", "관리자만 이용할 수 있습니다.");
	}
}
