package com.bukang.payout.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 판매자 정산을 조회한 회원이 존재하지 않거나 탈퇴했다 (회원 서비스가 404로 답함)
public class PayoutSellerMemberNotFoundException extends BusinessException {
	public PayoutSellerMemberNotFoundException() {
		super(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다.");
	}
}
