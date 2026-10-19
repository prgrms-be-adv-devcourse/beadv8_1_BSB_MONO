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
import lombok.extern.slf4j.Slf4j;

// PRO-215: Toss 승인(느린 외부 호출)은 트랜잭션 밖에서 부르고, 지갑 반영은 그 결과를 들고 짧은 트랜잭션에서
// 비관적 락(CashSupport.lockWallet)으로 보호한다. 그래야 Toss를 기다리는 동안 열어 둔 트랜잭션이 다른
// 트랜잭션과 버전 충돌을 일으켜, 이미 끝난 Toss 승인이 롤백으로 기록을 잃는 상황을 막을 수 있다.
@Slf4j
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
		CashChargeResponseDto existingCompleted = transactionTemplate.execute(
			status -> findExistingCompletedLocked(customerId, orderId));
		if (existingCompleted != null) {
			return existingCompleted;
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

		try {
			return transactionTemplate.execute(status -> recordCompletedLocked(customerId, orderId, amount, result));
		} catch (RuntimeException e) {
			// Toss 승인은 끝나 돈이 이미 빠진 뒤라, 이 트랜잭션이 DB 문제로 실패하면 기록 없이 돈만 빠진다.
			// 자동 복구 수단이 없으니, 수동으로 맞출 수 있게 단서를 남긴다 (PR 리뷰 제안)
			log.error("Toss 승인 후 지갑 반영이 실패했습니다. 수동 확인 필요. orderId={}, paymentKey={}, pgPaymentKey={}",
				orderId, paymentKey, result.pgPaymentKey(), e);
			throw e;
		}
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

	// 지갑을 잠그고, 이 orderId로 이미 COMPLETED인 충전이 있으면 그 결과를 돌려준다 (Toss를 부르기 전 조기 반환).
	// FAILED는 조기 반환하지 않는다 — 연결 실패(타임아웃)처럼 Toss 쪽 실제 결과를 모르는 채로 실패 처리됐을
	// 수 있어서, Toss를 다시 불러봐야 진짜 상태를 알 수 있다. 다시 불렀을 때 실제로 이미 처리된 거였다면
	// ALREADY_PROCESSED_PAYMENT로 거절되고, recoverIfAlreadyProcessed가 재조회로 COMPLETED까지 복구한다
	// (PR 리뷰 지적). 다른 회원 소유인지 확인하는 멱등키 검증은 상태와 상관없이 여기서 그대로 한다
	private CashChargeResponseDto findExistingCompletedLocked(int customerId, String orderId) {
		Wallet customerWallet = cashSupport.lockWallet(customerId);
		WalletCharge existing = findMatchingCharge(customerWallet, orderId);
		if (existing == null || existing.getStatus() != ChargeStatus.COMPLETED) {
			return null;
		}
		return toDto(existing, customerWallet.getBalance());
	}

	// 지갑을 다시 잠그고(짧은 트랜잭션), 그사이 같은 orderId로 먼저 끝난 요청이 없으면 FAILED로 기록한다
	// 이미 기록이 있으면(COMPLETED든 FAILED든) 그대로 돌려준다 — 이 호출 자체가 실패했으니 새로 알려줄 성공이 없다
	private CashChargeResponseDto recordFailedLocked(int customerId, String orderId, int amount) {
		Wallet customerWallet = cashSupport.lockWallet(customerId);
		WalletCharge existing = findMatchingCharge(customerWallet, orderId);
		if (existing != null) {
			return toDto(existing, customerWallet.getBalance());
		}
		WalletCharge charge = walletChargeRepository.save(new WalletCharge(customerWallet, amount, "TOSS", orderId));
		charge.markFailed();
		return CashChargeResponseDto.failed(customerWallet.getBalance());
	}

	// 지갑을 다시 잠그고(짧은 트랜잭션) COMPLETED로 기록하고 적립한다.
	// 기존 기록이 COMPLETED면 그대로 멱등 반환하지만, FAILED면 그대로 믿지 않는다 — 같은 orderId로 동시에
	// 들어온 다른 요청이 먼저 거절당해 남긴 기록일 수 있는데, 지금 이 호출은 실제로 Toss 승인을 받았으므로
	// 그 성공을 무시하면 돈은 빠졌는데 기록이 없는 상황이 된다 (PR 리뷰 지적). 기존 FAILED 행을 재사용해
	// COMPLETED로 갱신한다
	private CashChargeResponseDto recordCompletedLocked(
		int customerId, String orderId, int amount, PaymentApprovalResult result
	) {
		Wallet customerWallet = cashSupport.lockWallet(customerId);
		WalletCharge existing = findMatchingCharge(customerWallet, orderId);
		if (existing != null && existing.getStatus() == ChargeStatus.COMPLETED) {
			return toDto(existing, customerWallet.getBalance());
		}
		WalletCharge charge = existing != null
			? existing
			: walletChargeRepository.save(new WalletCharge(customerWallet, amount, "TOSS", orderId));
		charge.markCompleted(result.pgPaymentKey(), LocalDateTime.now());
		walletLedgerRepository.save(customerWallet.credit(
			result.approvedAmount(), LedgerType.충전__PG결제_토스페이먼츠, charge));
		return CashChargeResponseDto.completed(customerWallet.getBalance());
	}

	private WalletCharge findMatchingCharge(Wallet customerWallet, String orderId) {
		WalletCharge existing = walletChargeRepository.findByIdempotencyKey(orderId).orElse(null);
		if (existing == null) {
			return null;
		}
		if (existing.getWallet().getId() != customerWallet.getId()) {
			throw new ChargeMemberMismatchException("이 orderId로 충전한 회원과 요청한 회원이 다릅니다.");
		}
		return existing;
	}

	private CashChargeResponseDto toDto(WalletCharge charge, int balance) {
		return charge.getStatus() == ChargeStatus.COMPLETED
			? CashChargeResponseDto.completed(balance)
			: CashChargeResponseDto.failed(balance);
	}
}
