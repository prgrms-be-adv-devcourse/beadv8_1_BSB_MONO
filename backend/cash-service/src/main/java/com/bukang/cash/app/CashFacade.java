package com.bukang.cash.app;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.cash.domain.member.CashMember;
import com.bukang.cash.domain.wallet.Wallet;
import com.bukang.cash.dto.CashChargeResponseDto;
import com.bukang.cash.dto.CashPaymentResponseDto;
import com.bukang.cash.dto.CashRefundResponseDto;
import com.bukang.cash.dto.CashWithdrawalResponseDto;
import com.bukang.cash.dto.WalletBalanceResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CashFacade {
	private final CashSupport cashSupport;
	private final CashSyncMemberUseCase cashSyncMemberUseCase;
	private final CashCreateWalletUseCase cashCreateWalletUseCase;
	private final CashCompleteOrderPaymentUseCase cashCompleteOrderPaymentUseCase;
	private final CashCancelOrderPaymentUseCase cashCancelOrderPaymentUseCase;
	private final CashRefundPaymentUseCase cashRefundPaymentUseCase;
	private final CashChargeConfirmUseCase cashChargeConfirmUseCase;
	private final CashWithdrawalUseCase cashWithdrawalUseCase;

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
	public WalletBalanceResponseDto getBalance(int holderId) {
		return WalletBalanceResponseDto.from(cashSupport.findWalletByHolderId(holderId));
	}

	@Transactional
	public CashPaymentResponseDto completeOrderPayment(int orderId, int customerId, int salePrice) {
		return cashCompleteOrderPaymentUseCase.completeOrderPayment(orderId, customerId, salePrice);
	}

	@Transactional
	public void cancelPayment(int orderId, int customerId) {
		cashCancelOrderPaymentUseCase.cancelPayment(orderId, customerId);
	}

	@Transactional
	public CashRefundResponseDto refund(int orderId, int customerId) {
		return cashRefundPaymentUseCase.refund(orderId, customerId);
	}

	@Transactional
	public CashChargeResponseDto confirmWalletCharge(int customerId, String paymentKey, String orderId, int amount) {
		return cashChargeConfirmUseCase.confirmWalletCharge(customerId, paymentKey, orderId, amount);
	}

	@Transactional
	public CashWithdrawalResponseDto requestWithdrawal(
		int customerId, String bankCode, String accountNumber, String accountHolder, String idempotencyKey
	) {
		return cashWithdrawalUseCase.requestWithdrawal(
			customerId, bankCode, accountNumber, accountHolder, idempotencyKey);
	}
}
