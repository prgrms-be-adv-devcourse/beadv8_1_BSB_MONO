package com.bukang.cash.domain.payment;

public enum PaymentStatus {
	PENDING,
	COMPLETED,
	FAILED,
	TIMED_OUT,
	REFUNDED,
	CANCELED // 시스템 환원(고객 취소 아님)
}
