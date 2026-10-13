package com.bukang.cash.domain;

// 결제 상태가 FAILED일 때만 채운다
public enum PaymentFailReason {
	INSUFFICIENT_BALANCE,
	PRICE_CHANGED,
	OUT_OF_STOCK,
	NOT_ON_SALE,
	DEADLINE_PASSED
}
