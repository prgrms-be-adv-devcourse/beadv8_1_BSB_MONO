package com.bukang.cash.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.bukang.cash.config.CashWalletProperties;
import com.bukang.cash.domain.payout.PayoutStage;
import com.bukang.cash.domain.payout.PayoutTransfer;
import com.bukang.cash.domain.payout.PayoutTransferType;
import com.bukang.cash.domain.wallet.LedgerRefType;
import com.bukang.cash.domain.wallet.LedgerType;
import com.bukang.cash.domain.wallet.Wallet;
import com.bukang.cash.dto.CashPayoutResponseDto;
import com.bukang.cash.out.PayoutTransferRepository;
import com.bukang.cash.out.WalletLedgerRepository;

import lombok.RequiredArgsConstructor;

/**
 * 정산 지급: 홀딩 지갑에서 판매자 지갑으로 대금을, 최종 정산이면 시스템 지갑으로 수수료도 함께 옮긴다 (PRO-256)
 * 최종 정산은 회수 상계로 대금이 0원이 될 수 있다. 이때도 수수료는 상계와 무관하게 그대로 옮기므로,
 * 대금·수수료 이동은 각각 있을 때만 처리하고(둘 다 0원인 호출은 없다고 전제) 판매자 지갑도 대금이 있을 때만 잠근다.
 * 대금 이동·수수료 이동은 각각 독립된 PayoutTransfer 행을 근거로 삼아 서로 다른 ref_id로 장부에 남긴다.
 */
@Service
@RequiredArgsConstructor
public class CashPayoutUseCase {
	private final CashSupport cashSupport;
	private final CashWalletProperties cashWalletProperties;
	private final PayoutTransferRepository payoutTransferRepository;
	private final WalletLedgerRepository walletLedgerRepository;

	public CashPayoutResponseDto payout(
		int payoutId, int sellerId, PayoutStage payoutStage, int settlementAmount, int feeAmount
	) {
		int holdingMemberId = cashWalletProperties.holdingMemberId();
		int systemMemberId = cashWalletProperties.systemMemberId();
		boolean withSettlement = settlementAmount > 0;
		// 1차 정산은 수수료가 없다. 최종 정산도 상계로 대금만 0원이 되고 수수료는 남을 수 있다 (완료 기준)
		boolean withFee = payoutStage == PayoutStage.FINAL && feeAmount > 0;

		// 1.지갑 잠금 (홀딩은 항상, 판매자·시스템은 실제로 돈이 오갈 때만, ID 작은 쪽부터)
		Map<Integer, Wallet> wallets = cashSupport.lockWallets(
			payoutWalletIds(holdingMemberId, sellerId, systemMemberId, withSettlement, withFee));
		Wallet holdingWallet = wallets.get(holdingMemberId);

		// 2.대금 이동 (홀딩 → 판매자, 대금이 있을 때만), 3.수수료 이동 (홀딩 → 시스템, 최종이고 수수료가 있을 때만)
		boolean settlementTransferred = withSettlement && transfer(payoutId, sellerId, PayoutTransferType.SETTLEMENT,
			settlementAmount, holdingWallet, wallets.get(sellerId), LedgerType.정산지급__상품판매_대금,
			LedgerType.정산수령__상품판매_대금);
		boolean feeTransferred = withFee && transfer(payoutId, sellerId, PayoutTransferType.FEE, feeAmount,
			holdingWallet, wallets.get(systemMemberId), LedgerType.정산지급__상품판매_수수료, LedgerType.정산수령__상품판매_수수료);

		int paidFeeAmount = withFee ? feeAmount : 0;
		return settlementTransferred || feeTransferred
			? CashPayoutResponseDto.paid(settlementAmount, paidFeeAmount)
			: CashPayoutResponseDto.alreadyPaid(settlementAmount, paidFeeAmount);
	}

	private int[] payoutWalletIds(
		int holdingMemberId, int sellerId, int systemMemberId, boolean withSettlement, boolean withFee
	) {
		List<Integer> holderIds = new ArrayList<>();
		holderIds.add(holdingMemberId);
		if (withSettlement) {
			holderIds.add(sellerId);
		}
		if (withFee) {
			holderIds.add(systemMemberId);
		}
		return holderIds.stream().mapToInt(Integer::intValue).toArray();
	}

	// 같은 payoutId·transferType으로 이미 옮긴 적 있으면 지갑을 건드리지 않고 false를 돌려준다 (멱등)
	private boolean transfer(
		int payoutId, int sellerId, PayoutTransferType transferType, int amount,
		Wallet fromWallet, Wallet toWallet, LedgerType debitType, LedgerType creditType
	) {
		if (payoutTransferRepository.existsByPayoutIdAndTransferType(payoutId, transferType)) {
			return false;
		}
		PayoutTransfer transferRecord = payoutTransferRepository.save(
			new PayoutTransfer(payoutId, sellerId, transferType, amount));
		walletLedgerRepository.save(
			fromWallet.debit(amount, debitType, LedgerRefType.Payout, transferRecord.getId()));
		walletLedgerRepository.save(
			toWallet.credit(amount, creditType, LedgerRefType.Payout, transferRecord.getId()));
		return true;
	}
}
