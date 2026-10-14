package com.bukang.cash.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import com.bukang.cash.config.CashWalletProperties;
import com.bukang.cash.domain.member.CashMember;
import com.bukang.cash.domain.payment.PaymentStatus;
import com.bukang.cash.domain.wallet.LedgerRefType;
import com.bukang.cash.domain.wallet.LedgerType;
import com.bukang.cash.domain.wallet.Wallet;
import com.bukang.cash.domain.wallet.WalletLedger;
import com.bukang.cash.out.CashMemberRepository;
import com.bukang.cash.out.PaymentRepository;
import com.bukang.cash.out.WalletLedgerRepository;
import com.bukang.cash.out.WalletRepository;

// PRO-83 완료 기준 중 동시성(동시 환불, 환원·환불 경합)은 MockMvc 없이 Facade를 직접 불러 확인한다
// (CashCancelOrderPaymentUseCaseTest와 같은 방식)
@SpringBootTest
@ActiveProfiles("test")
class CashRefundPaymentUseCaseTest {
	private static final int CUSTOMER_ID = 3;

	@Autowired
	private CashFacade cashFacade;
	@Autowired
	private CashWalletProperties cashWalletProperties;
	@Autowired
	private CashMemberRepository cashMemberRepository;
	@Autowired
	private WalletRepository walletRepository;
	@Autowired
	private WalletLedgerRepository walletLedgerRepository;
	@Autowired
	private PaymentRepository paymentRepository;
	@Autowired
	private TransactionTemplate transactionTemplate;

	@AfterEach
	void tearDown() {
		paymentRepository.deleteAll();
		walletLedgerRepository.deleteAll();
		walletRepository.deleteAll();
		cashMemberRepository.deleteAll();
	}

	@Test
	@DisplayName("환원(cancelPayment)된 결제를 환불하면 아무것도 바뀌지 않고 성공한다")
	void refundCanceledPayment_noDoubleReturn() {
		int orderId = 500;
		int amount = 3_000;
		createCustomerWallet(10_000);
		createHoldingWallet();
		cashFacade.completeOrderPayment(orderId, CUSTOMER_ID, amount);
		cashFacade.cancelPayment(orderId, CUSTOMER_ID);

		cashFacade.refund(orderId, CUSTOMER_ID);

		assertBalance(CUSTOMER_ID, 10_000);
		assertThat(countLedgersForOrder(orderId)).as("결제 2줄 + 환원 2줄, 환불은 추가로 남기면 안 된다").isEqualTo(4);
		assertThat(paymentRepository.findByOrderId(orderId).orElseThrow().getStatus())
			.as("CANCELED는 그대로 유지돼야 한다(REFUNDED로 덮어쓰면 안 됨)")
			.isEqualTo(PaymentStatus.CANCELED);
	}

	@Test
	@DisplayName("같은 주문으로 10개 스레드가 동시에 환불을 요청해도 반환은 한 번만 일어난다")
	void concurrentRefundSameOrder_refundsOnlyOnce() throws InterruptedException {
		int orderId = 600;
		int amount = 3_000;
		createCustomerWallet(10_000);
		createHoldingWallet();
		cashFacade.completeOrderPayment(orderId, CUSTOMER_ID, amount);

		List<Throwable> errors = runConcurrently(10, () -> cashFacade.refund(orderId, CUSTOMER_ID));

		assertThat(errors).as("예외 없이 전부 처리돼야 한다").isEmpty();
		assertBalance(CUSTOMER_ID, 10_000);
		assertBalance(cashWalletProperties.holdingMemberId(), 0);
		assertThat(countLedgersForOrder(orderId)).as("결제 2줄 + 환불 2줄만 있어야 한다").isEqualTo(4);
		assertThat(paymentRepository.findByOrderId(orderId).orElseThrow().getStatus())
			.isEqualTo(PaymentStatus.REFUNDED);
	}

	@Test
	@DisplayName("같은 주문에 환원과 환불이 동시에 와도 한쪽만 반영된다")
	void concurrentCancelAndRefund_onlyOneWins() throws InterruptedException {
		int orderId = 700;
		int amount = 3_000;
		createCustomerWallet(10_000);
		createHoldingWallet();
		cashFacade.completeOrderPayment(orderId, CUSTOMER_ID, amount);

		CountDownLatch startSignal = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		Future<?> cancelFuture = executor.submit(() -> {
			await(startSignal);
			cashFacade.cancelPayment(orderId, CUSTOMER_ID);
		});
		Future<?> refundFuture = executor.submit(() -> {
			await(startSignal);
			cashFacade.refund(orderId, CUSTOMER_ID);
		});
		startSignal.countDown();
		join(cancelFuture);
		join(refundFuture);
		executor.shutdown();

		// 어느 쪽이 먼저 끝나든, 결제 금액은 딱 한 번만 돌아와야 한다
		assertBalance(CUSTOMER_ID, 10_000);
		assertBalance(cashWalletProperties.holdingMemberId(), 0);
		assertThat(countLedgersForOrder(orderId)).as("결제 2줄 + 반환 2줄만 있어야 한다(이중 반환 없음)").isEqualTo(4);
		PaymentStatus finalStatus = paymentRepository.findByOrderId(orderId).orElseThrow().getStatus();
		assertThat(finalStatus).isIn(PaymentStatus.CANCELED, PaymentStatus.REFUNDED);
		assertLedgerConsistency(CUSTOMER_ID, cashWalletProperties.holdingMemberId());
	}

	private List<Throwable> runConcurrently(int threadCount, Runnable task) throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch startSignal = new CountDownLatch(1);
		List<Future<Void>> futures = new ArrayList<>();
		for (int i = 0; i < threadCount; i++) {
			Callable<Void> callable = () -> {
				startSignal.await();
				task.run();
				return null;
			};
			futures.add(executor.submit(callable));
		}
		startSignal.countDown();

		List<Throwable> errors = new ArrayList<>();
		for (Future<Void> future : futures) {
			try {
				future.get();
			} catch (ExecutionException e) {
				errors.add(e.getCause());
			}
		}
		executor.shutdown();
		return errors;
	}

	private void await(CountDownLatch latch) {
		try {
			latch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	private void join(Future<?> future) {
		try {
			future.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException ignored) {
			// 각 작업 안에서 이미 처리했으므로 여기서는 무시한다
		}
	}

	private void assertBalance(int holderId, int expected) {
		assertThat(walletRepository.findById(holderId).orElseThrow().getBalance()).isEqualTo(expected);
	}

	private long countLedgersForOrder(int orderId) {
		return walletLedgerRepository.findAll().stream()
			.filter(ledger -> ledger.getRefTypeCode() == LedgerRefType.Order && ledger.getRefId() == orderId)
			.count();
	}

	private void assertLedgerConsistency(int... holderIds) {
		for (int holderId : holderIds) {
			int walletBalance = walletRepository.findById(holderId).orElseThrow().getBalance();
			int ledgerSum = walletLedgerRepository.findAll().stream()
				.filter(ledger -> ledger.getWallet().getId() == holderId)
				.mapToInt(WalletLedger::getAmount)
				.sum();
			assertThat(walletBalance).as("지갑 %d 잔액은 그 지갑 장부 합과 같아야 한다", holderId).isEqualTo(ledgerSum);
		}
	}

	private Wallet createCustomerWallet(int initialBalance) {
		return createWallet(CUSTOMER_ID, "러너3", initialBalance);
	}

	private void createHoldingWallet() {
		createWallet(cashWalletProperties.holdingMemberId(), "홀딩", 0);
	}

	private Wallet createWallet(int memberId, String nickname, int initialBalance) {
		return transactionTemplate.execute(status -> {
			LocalDateTime now = LocalDateTime.of(2026, 10, 12, 10, 0);
			CashMember member = cashMemberRepository.save(new CashMember(memberId, now, now, nickname));
			Wallet wallet = walletRepository.save(new Wallet(member));
			if (initialBalance != 0) {
				walletLedgerRepository.save(
					wallet.credit(initialBalance, LedgerType.충전__PG결제_토스페이먼츠, LedgerRefType.WalletCharge, memberId));
			}
			return wallet;
		});
	}
}
