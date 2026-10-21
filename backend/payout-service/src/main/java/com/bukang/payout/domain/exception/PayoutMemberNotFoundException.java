package com.bukang.payout.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 관리자 API를 부른 회원이 회원 서비스에 없거나 탈퇴했다 (PRO-265). 회원 서비스의 관리자 API와 같이 404로 나눈다
public class PayoutMemberNotFoundException extends BusinessException {
	public PayoutMemberNotFoundException() {
		super(HttpStatus.NOT_FOUND, "PAYOUT_MEMBER_NOT_FOUND", "존재하지 않는 회원입니다.");
	}
}
