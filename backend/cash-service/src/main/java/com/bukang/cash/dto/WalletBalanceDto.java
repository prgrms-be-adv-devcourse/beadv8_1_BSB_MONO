package com.bukang.cash.dto;

import com.bukang.cash.domain.Wallet;

/**
 * 지갑 잔액 조회 응답
 */
public record WalletBalanceDto(
	int holderId,
	long balance
) {
	public static WalletBalanceDto from(Wallet wallet) {
		return new WalletBalanceDto(wallet.getId(), wallet.getBalance());
	}
}
