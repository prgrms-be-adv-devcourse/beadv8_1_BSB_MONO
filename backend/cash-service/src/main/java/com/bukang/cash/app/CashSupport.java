package com.bukang.cash.app;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.bukang.cash.config.CashWalletProperties;
import com.bukang.cash.domain.exception.WalletNotFoundException;
import com.bukang.cash.domain.payment.Payment;
import com.bukang.cash.domain.wallet.Wallet;
import com.bukang.cash.out.PaymentRepository;
import com.bukang.cash.out.WalletRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CashSupport {
	private final WalletRepository walletRepository;
	private final PaymentRepository paymentRepository;
	private final CashWalletProperties cashWalletProperties;

	// 지갑 ID = 회원 ID. 지갑은 가입 이벤트로 만들어지므로, 없으면 조회 시점에 만들지 않고 404로 알린다
	public Wallet findWalletByHolderId(int holderId) {
		return walletRepository.findById(holderId)
			.orElseThrow(() -> new WalletNotFoundException("존재하지 않는 지갑입니다."));
	}

	// 홀딩 지갑도 회원 가입 이벤트로 만들어진다 (member-service가 1·2번을 시스템·홀딩 회원으로 둔다)
	public Wallet findHoldingWallet() {
		return findWalletByHolderId(cashWalletProperties.holdingMemberId());
	}

	public Wallet findSystemWallet() {
		return findWalletByHolderId(cashWalletProperties.systemMemberId());
	}

	// 회원 탈퇴 확인용 잔액 (BE-022). 지갑이 없으면 남은 예치금도 없으므로 404 대신 0원으로 본다
	public int findBalanceOrZero(int holderId) {
		return walletRepository.findById(holderId)
			.map(Wallet::getBalance)
			.orElse(0);
	}

	// 잠그지 않고 있는지만 본다 (환원처럼 "지갑이 없으면 애초에 결제된 적도 없다"를 판단할 때 쓴다)
	public boolean hasWallet(int holderId) {
		return walletRepository.existsById(holderId);
	}

	//주문 존재 확인
	public Optional<Payment> findByOrderId(int orderId) {
		return paymentRepository.findByOrderId(orderId);
	}

	// 지갑 하나만 비관적 락으로 잠근다 (충전 확정처럼 다른 지갑과 같이 잠글 필요가 없을 때 쓴다, PRO-215)
	public Wallet lockWallet(int holderId) {
		return walletRepository.findByIdForUpdate(holderId)
			.orElseThrow(() -> new WalletNotFoundException("존재하지 않는 지갑입니다."));
	}

	// 고객 지갑·보관 지갑을 ID가 작은 쪽부터 비관적 락으로 잠근다 (교착 상태 방지, PRO-78)
	// 지갑 ID가 회원 ID와 항상 같을 때만 올바르다. 지갑 ID가 자동 증가(BaseIdAndTime)라면
	// 다른 회원의 지갑을 잠글 수 있으므로, 회원 ID로 잠그는 findByMemberIdForUpdate를 써야 한다.
	public LockedWallets lockCustomerAndHoldingWallets(int customerId) {
		int holdingMemberId = cashWalletProperties.holdingMemberId();
		int firstId = Math.min(customerId, holdingMemberId);
		int secondId = Math.max(customerId, holdingMemberId);

		Wallet first = walletRepository.findByIdForUpdate(firstId)
			.orElseThrow(() -> new WalletNotFoundException("존재하지 않는 지갑입니다."));
		Wallet second = walletRepository.findByIdForUpdate(secondId)
			.orElseThrow(() -> new WalletNotFoundException("존재하지 않는 지갑입니다."));

		Wallet customerWallet = customerId == firstId ? first : second;
		Wallet holdingWallet = holdingMemberId == firstId ? first : second;
		return new LockedWallets(customerWallet, holdingWallet);
	}

	public record LockedWallets(Wallet customerWallet, Wallet holdingWallet) {
	}

}
