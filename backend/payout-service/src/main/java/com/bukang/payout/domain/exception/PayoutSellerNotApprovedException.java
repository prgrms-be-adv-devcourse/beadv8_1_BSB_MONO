package com.bukang.payout.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 판매자 신청이 없거나 대기 · 반려 상태인 회원이 판매자 정산을 조회했다
public class PayoutSellerNotApprovedException extends BusinessException {
	public PayoutSellerNotApprovedException() {
		super(HttpStatus.FORBIDDEN, "승인된 판매자만 정산을 조회할 수 있습니다.");
	}
}
