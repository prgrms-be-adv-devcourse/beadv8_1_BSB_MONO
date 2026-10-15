package com.bukang.cash.app;

import org.springframework.stereotype.Service;

import com.bukang.cash.app.CashSupport.LockedWallets;
import com.bukang.cash.domain.exception.PaymentMemberMismatchException;
import com.bukang.cash.domain.exception.PaymentNotRefundableException;
import com.bukang.cash.domain.payment.Payment;
import com.bukang.cash.domain.payment.PaymentStatus;
import com.bukang.cash.domain.wallet.LedgerRefType;
import com.bukang.cash.domain.wallet.LedgerType;
import com.bukang.cash.domain.wallet.Wallet;
import com.bukang.cash.dto.CashRefundDto;
import com.bukang.cash.out.WalletLedgerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CashRefundPaymentUseCase {
	private final CashSupport cashSupport;
	private final WalletLedgerRepository walletLedgerRepository;

	// 결제 완료된 주문만 환불한다. cancelPayment와 달리 결제 전 주문은 선점하지 않고 그냥 예외로 거절한다
	// (환불은 고객이 끝난 결제를 취소할 때만 부르는 것이라, 결제된 적 없는 주문엔 돌려줄 게 없다)
	public CashRefundDto refund(int orderId, int customerId) {
		LockedWallets lockedWallets = cashSupport.lockCustomerAndHoldingWallets(customerId);
		Wallet customerWallet = lockedWallets.customerWallet();
		Wallet holdingWallet = lockedWallets.holdingWallet();

		Payment payment = cashSupport.findByOrderId(orderId)
			.orElseThrow(() -> new PaymentNotRefundableException("환불할 결제가 없습니다."));

		// 결제 행의 실제 주인(지갑)과 요청받은 회원이 다르면 거절한다 (다른 회원 지갑으로 환불되는 것을 막음)
		if (payment.getWallet().getId() != customerWallet.getId()) {
			throw new PaymentMemberMismatchException("결제한 회원과 요청한 회원이 다릅니다.");
		}

		// 이미 환원·환불됐으면 그대로 끝 (멱등) — 환원과 환불이 동시에 와도 한쪽만 반영된다
		if (payment.getStatus() == PaymentStatus.CANCELED || payment.getStatus() == PaymentStatus.REFUNDED) {
			return new CashRefundDto(payment.getAmount());
		}

		if (payment.getStatus() != PaymentStatus.COMPLETED) {
			throw new PaymentNotRefundableException("결제가 완료되지 않아 환불할 수 없습니다.");
		}

		int amount = payment.getAmount();
		walletLedgerRepository.save(
			customerWallet.credit(amount, LedgerType.환불__주문취소, LedgerRefType.Order, orderId));
		walletLedgerRepository.save(
			holdingWallet.debit(amount, LedgerType.임시보관반환__주문취소, LedgerRefType.Order, orderId));

		payment.markRefunded();
		return new CashRefundDto(amount);
	}
}
