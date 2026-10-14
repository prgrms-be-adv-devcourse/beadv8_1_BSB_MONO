package com.bukang.cash.dto;

public record CashPaymentDto(Outcome outcome, int shortage) {
	public enum Outcome {
		PAID,
		INSUFFICIENT_BALANCE
	}

	public static CashPaymentDto paid() {
		return new CashPaymentDto(Outcome.PAID, 0);
	}

	public static CashPaymentDto insufficient(int shortage) {
		return new CashPaymentDto(Outcome.INSUFFICIENT_BALANCE, shortage);
	}

	public String message() {
		return outcome() == Outcome.PAID ? "결제했습니다." : "예치금이 부족합니다.";
	}
}
