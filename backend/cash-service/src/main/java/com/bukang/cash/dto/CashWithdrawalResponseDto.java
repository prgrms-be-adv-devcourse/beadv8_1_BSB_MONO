package com.bukang.cash.dto;

public record CashWithdrawalResponseDto(int withdrawnAmount, int balance) {
	public static CashWithdrawalResponseDto of(int withdrawnAmount, int balance) {
		return new CashWithdrawalResponseDto(withdrawnAmount, balance);
	}

	public String message() {
		return "환급 신청이 완료되었습니다.";
	}
}
