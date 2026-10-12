package com.bukang.cash.domain;

// 환급 상태가 FAILED일 때만 채운다. ERD에 예시(INVALID_ACCOUNT)만 있어 사유가 정해지면 추가한다
public enum WithdrawalFailReason {
	INVALID_ACCOUNT
}
