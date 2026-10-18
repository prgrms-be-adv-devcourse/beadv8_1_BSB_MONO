package com.bukang.cash.app;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.bukang.cash.domain.charge.ChargeStatus;
import com.bukang.cash.domain.charge.WalletCharge;
import com.bukang.cash.domain.exception.ChargeAmountTooSmallException;
import com.bukang.cash.domain.exception.ChargeMemberMismatchException;
import com.bukang.cash.domain.exception.PaymentApprovalFailedException;
import com.bukang.cash.domain.wallet.LedgerType;
import com.bukang.cash.domain.wallet.Wallet;
import com.bukang.cash.dto.CashChargeResponseDto;
import com.bukang.cash.out.WalletChargeRepository;
import com.bukang.cash.out.WalletLedgerRepository;
import com.bukang.cash.out.pg.PaymentApprovalResult;
import com.bukang.cash.out.pg.PaymentClient;

import lombok.RequiredArgsConstructor;

// PRO-215: Toss 승인(느린 외부 호출)은 트랜잭션 밖에서 부르고, 지갑 반영은 그 결과를 들고 짧은 트랜잭션에서
// 비관적 락(CashSupport.lockWallet)으로 보호한다. 그래야 Toss를 기다리는 동안 열어 둔 트랜잭션이 다른
// 트랜잭션과 버전 충돌을 일으켜, 이미 끝난 Toss 승인이 롤백으로 기록을 잃는 상황을 막을 수 있다.
@Service
@RequiredArgsConstructor
public class CashChargeConfirmUseCase {
	private static final int MINIMUM_AMOUNT = 10_000;
	// Toss가 confirm을 거절하면서 "이미 처리가 끝난 결제"라고 알리는 코드 (PRO-200)
	// 타임아웃 뒤 재시도에서만 받을 수 있다 — 먼저 보낸 요청이 Toss 쪽에서는 실제로 승인됐을 수 있다는 뜻이다
	private static final String ALREADY_PROCESSED_PAYMENT = "ALREADY_PROCESSED_PAYMENT";

	private final CashSupport cashSupport;
	private final WalletChargeRepository walletChargeRepository;
	private final WalletLedgerRepository walletLedgerRepository;
	private final PaymentClient paymentClient;
	private final TransactionTemplate transactionTemplate;

	public CashChargeResponseDto confirmWalletCharge(int customerId, String paymentKey, String orderId, int amount) {
		CashChargeResponseDto existing = transactionTemplate.execute(
			status -> findExistingLocked(customerId, orderId));
		if (existing != null) {
			return existing;
		}

		if (amount < MINIMUM_AMOUNT) {
			throw new ChargeAmountTooSmallException("최소 충전 금액은 10,000원입니다.");
		}

		PaymentApprovalResult result;
		try {
			result = paymentClient.approve(paymentKey, orderId, amount);
		} catch (PaymentApprovalFailedException failure) {
			Optional<PaymentApprovalResult> actuallyCompleted = recoverIfAlreadyProcessed(failure, orderId);
			if (actuallyCompleted.isPresent()) {
				PaymentApprovalResult recovered = actuallyCompleted.get();
				return transactionTemplate.execute(
					status -> recordCompletedLocked(customerId, orderId, amount, recovered));
			}
			return transactionTemplate.execute(status -> recordFailedLocked(customerId, orderId, amount));
		}

		return transactionTemplate.execute(status -> recordCompletedLocked(customerId, orderId, amount, result));
	}

	// Toss가 "이미 처리된 결제"라고 거절했을 때만, 실제로 승인까지 끝났는지 재조회해서 확인한다
	private Optional<PaymentApprovalResult> recoverIfAlreadyProcessed(
		PaymentApprovalFailedException failure, String orderId
	) {
		if (!ALREADY_PROCESSED_PAYMENT.equals(failure.getPgCode())) {
			return Optional.empty();
		}
		return paymentClient.findCompletedByOrderId(orderId);
	}

	// 지갑을 잠그고, 이 orderId로 이미 끝난(성공·실패) 충전이 있으면 그 결과를 돌려준다 (Toss를 부르기 전 조기 반환)
	private CashChargeResponseDto findExistingLocked(int customerId, String orderId) {
		Wallet customerWallet = cashSupport.lockWallet(customerId);
		return existingResultOrNull(customerWallet, orderId);
	}

	// 지갑을 다시 잠그고(짧은 트랜잭션), 그사이 같은 orderId로 먼저 끝난 요청이 없으면 FAILED로 기록한다
	private CashChargeResponseDto recordFailedLocked(int customerId, String orderId, int amount) {
		Wallet customerWallet = cashSupport.lockWallet(customerId);
		CashChargeResponseDto existingResult = existingResultOrNull(customerWallet, orderId);
		if (existingResult != null) {
			return existingResult;
		}
		WalletCharge charge = walletChargeRepository.save(new WalletCharge(customerWallet, amount, "TOSS", orderId));
		charge.markFailed();
		return CashChargeResponseDto.failed(customerWallet.getBalance());
	}

	// 지갑을 다시 잠그고(짧은 트랜잭션), 그사이 같은 orderId로 먼저 끝난 요청이 없으면 COMPLETED로 기록하고 적립한다
	private CashChargeResponseDto recordCompletedLocked(
		int customerId, String orderId, int amount, PaymentApprovalResult result
	) {
		Wallet customerWallet = cashSupport.lockWallet(customerId);
		CashChargeResponseDto existingResult = existingResultOrNull(customerWallet, orderId);
		if (existingResult != null) {
			return existingResult;
		}
		WalletCharge charge = walletChargeRepository.save(new WalletCharge(customerWallet, amount, "TOSS", orderId));
		charge.markCompleted(result.pgPaymentKey(), LocalDateTime.now());
		walletLedgerRepository.save(customerWallet.credit(
			result.approvedAmount(), LedgerType.충전__PG결제_토스페이먼츠, charge));
		return CashChargeResponseDto.completed(customerWallet.getBalance());
	}

	private CashChargeResponseDto existingResultOrNull(Wallet customerWallet, String orderId) {
		WalletCharge existing = walletChargeRepository.findByIdempotencyKey(orderId).orElse(null);
		if (existing == null) {
			return null;
		}
		if (existing.getWallet().getId() != customerWallet.getId()) {
			throw new ChargeMemberMismatchException("이 orderId로 충전한 회원과 요청한 회원이 다릅니다.");
		}
		return toDto(existing, customerWallet.getBalance());
	}

	private CashChargeResponseDto toDto(WalletCharge charge, int balance) {
		return charge.getStatus() == ChargeStatus.COMPLETED
			? CashChargeResponseDto.completed(balance)
			: CashChargeResponseDto.failed(balance);
	}
}
