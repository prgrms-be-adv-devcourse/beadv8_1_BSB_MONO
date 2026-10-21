package com.bukang.payout.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 회원 서비스가 응답하지 않아 관리자인지 확인하지 못했다 (PRO-265)
// 확인하지 못한 요청을 통과시키면 그동안 일반 회원도 보류를 걸 수 있으므로 막고, 잠시 뒤 다시 시도하라는 뜻으로 503을 준다
public class PayoutMemberUnavailableException extends BusinessException {
	public PayoutMemberUnavailableException(Throwable cause) {
		super(HttpStatus.SERVICE_UNAVAILABLE, "PAYOUT_MEMBER_UNAVAILABLE",
			"회원 서비스에 연결하지 못해 관리자인지 확인하지 못했습니다. 잠시 뒤 다시 시도해주세요.");
		initCause(cause);
	}
}
