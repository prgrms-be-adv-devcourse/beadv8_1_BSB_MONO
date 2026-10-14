package com.bukang.cash.app;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.bukang.cash.config.CashWalletProperties;
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
import com.bukang.cash.out.WalletRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CashCompleteOrderPaymentUseCase {
	private final CashSupport cashSupport;
	private final PaymentRepository paymentRepository;
	private final WalletRepository walletRepository;
	private final WalletLedgerRepository walletLedgerRepository;
	private final CashWalletProperties cashWalletProperties;

	public CashPaymentDto completeOrderPayment(int orderId, int customerId, int salePrice) {
		// 1. 보관 지갑 주인(보관 회원)의 ID를 설정에서 가져온다
		int holdingMemberId = cashWalletProperties.holdingMemberId();

		// 2. 잠글 순서를 정한다
		//    결제·환원·환불이 지갑 두 개를 서로 다른 순서로 잠그면 교착 상태가 생기므로,
		//    항상 ID가 작은 쪽부터 잠근다. 결제·환원·환불 모두 같은 기준(여기서는 회원 ID)으로 순서를 정해야 한다.
		int firstId = Math.min(customerId, holdingMemberId);
		int secondId = Math.max(customerId, holdingMemberId);

		// 3. 정한 순서대로 지갑을 잠근다 (SELECT ... FOR UPDATE)
		//    잠그기 전에 지갑을 따로 읽지 않으므로, 잠그면서 처음으로 최신 잔액을 읽는다
		//    다른 요청은 이 트랜잭션이 커밋될 때까지 같은 지갑을 바꾸지 못하고 기다린다.
		//    지갑 ID가 회원 ID와 항상 같을 때만 올바르다.
		//    지갑 ID가 자동 증가(BaseIdAndTime)라면 다른 회원의 지갑을 잠글 수 있으므로,
		//    회원 ID로 잠그는 findByMemberIdForUpdate를 써야 한다.
		Wallet first = walletRepository.findByIdForUpdate(firstId)
			.orElseThrow(() -> new WalletNotFoundException("존재하지 않는 지갑입니다."));
		Wallet second = walletRepository.findByIdForUpdate(secondId)
			.orElseThrow(() -> new WalletNotFoundException("존재하지 않는 지갑입니다."));

		// 4. 잠근 지갑 중 어느 쪽이 고객 지갑이고 어느 쪽이 보관 지갑인지 짝지어 준다
		//    이후 차감·적립은 반드시 이 잠근 객체로 한다.
		Wallet customerWallet = customerId == firstId ? first : second;
		Wallet holdingWallet = holdingMemberId == firstId ? first : second;

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
