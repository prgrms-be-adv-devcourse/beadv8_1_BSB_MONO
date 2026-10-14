package com.bukang.cash.app;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.cash.domain.member.CashMember;
import com.bukang.cash.domain.wallet.Wallet;
import com.bukang.cash.dto.CashPaymentDto;
import com.bukang.cash.dto.WalletBalanceDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CashFacade {
	private final CashSupport cashSupport;
	private final CashSyncMemberUseCase cashSyncMemberUseCase;
	private final CashCreateWalletUseCase cashCreateWalletUseCase;
	private final CashCompleteOrderPaymentUseCase cashCompleteOrderPaymentUseCase;

	@Transactional
	public CashMember syncMember(int id, LocalDateTime createDate, LocalDateTime modifyDate,
		String nickname) {
		return cashSyncMemberUseCase.syncMember(id, createDate, modifyDate, nickname);
	}

	@Transactional
	public Wallet createWallet(int id) {
		return cashCreateWalletUseCase.createWallet(id);
	}

	@Transactional(readOnly = true)
	public WalletBalanceDto getBalance(int holderId) {
		return WalletBalanceDto.from(cashSupport.findWalletByHolderId(holderId));
	}

	@Transactional
	public CashPaymentDto completeOrderPayment(int orderId, int customerId, int salePrice) {
		return cashCompleteOrderPaymentUseCase.completeOrderPayment(orderId, customerId, salePrice);
	}
}
