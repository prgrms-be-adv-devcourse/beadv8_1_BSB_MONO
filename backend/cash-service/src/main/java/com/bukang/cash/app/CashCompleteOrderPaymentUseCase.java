package com.bukang.cash.app;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import com.bukang.cash.config.CashWalletProperties;
import com.bukang.cash.app.CashSupport.LockedWallets;
import com.bukang.cash.domain.exception.PaymentAlreadyFinalizedException;
import com.bukang.cash.domain.exception.WalletNotFoundException;
import com.bukang.cash.domain.payment.Payment;
import com.bukang.cash.domain.payment.PaymentFailReason;
import com.bukang.cash.domain.payment.PaymentStatus;
import com.bukang.cash.domain.wallet.LedgerRefType;
import com.bukang.cash.domain.wallet.LedgerType;
import com.bukang.cash.domain.wallet.Wallet;
import com.bukang.cash.dto.CashPaymentDto;
import com.bukang.cash.out.PaymentRepository;
import com.bukang.cash.out.WalletLedgerRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CashCompleteOrderPaymentUseCase {
	private final CashSupport cashSupport;
	private final PaymentRepository paymentRepository;
	private final WalletLedgerRepository walletLedgerRepository;

	public CashPaymentDto completeOrderPayment(int orderId, int customerId, int salePrice) {
		//1.지갑 잠금 (고객 지갑, 보관 지갑 / ID 작은 쪽부터)
		LockedWallets lockedWallets = cashSupport.lockCustomerAndHoldingWallets(customerId);
		Wallet customerWallet = lockedWallets.customerWallet();
		Wallet holdingWallet = lockedWallets.holdingWallet();

		Optional<Payment> existingPayment = cashSupport.findByOrderId(orderId);
		Payment payment = existingPayment.isPresent()
			? existingPayment.get()
			: paymentRepository.save(new Payment(orderId, customerWallet, salePrice));

		//2.orderId 존재할떄 상태 검증
		//Complete : 이미 끝
		//이미 CANCELED(주문 후 실패 해서 결제 취소) / REFUNDED(환불에서): 이미 처리됨 → 그대로 끝 (멱등)
		//Failed이거나 방금 생성된 주문이면 여기까지 내려옴
		if (payment.getStatus() == PaymentStatus.COMPLETED) {
			return CashPaymentDto.paid();
		}
		if (payment.getStatus() == PaymentStatus.CANCELED || payment.getStatus() == PaymentStatus.REFUNDED) {
			throw new PaymentAlreadyFinalizedException("이미 취소·환불된 결제입니다.");
		}

		//3.잔액 확인
		//예치금 부족 시 충전 요청 (지금 결제 상태 FAILED로 바꾸고 부족 금액 반환/ 커밋, 충전은 프론트가 따로 요청)
		if (customerWallet.getBalance() < salePrice) {
			payment.markFailed(PaymentFailReason.INSUFFICIENT_BALANCE);
			return CashPaymentDto.insufficient(salePrice - customerWallet.getBalance());
		}

		//4.예치금 차감, 장부 2줄 작성
		walletLedgerRepository.save(
			customerWallet.debit(salePrice, LedgerType.사용__주문결제, LedgerRefType.Order, orderId));
		walletLedgerRepository.save(
			holdingWallet.credit(salePrice, LedgerType.임시보관__주문결제, LedgerRefType.Order, orderId));

		//5.결제 상태 Complete
		payment.markCompleted(LocalDateTime.now());
		return CashPaymentDto.paid();
	}
}
