package com.bukang.cash.dto;

public record CashPayoutResponseDto(Outcome outcome, int settlementAmount, int feeAmount) {
	public enum Outcome {
		PAID,
		ALREADY_PAID
	}

	public static CashPayoutResponseDto paid(int settlementAmount, int feeAmount) {
		return new CashPayoutResponseDto(Outcome.PAID, settlementAmount, feeAmount);
	}

	public static CashPayoutResponseDto alreadyPaid(int settlementAmount, int feeAmount) {
		return new CashPayoutResponseDto(Outcome.ALREADY_PAID, settlementAmount, feeAmount);
	}

	public String message() {
		return outcome() == Outcome.PAID ? "정산금을 지급했습니다." : "이미 지급된 정산입니다.";
	}
}
