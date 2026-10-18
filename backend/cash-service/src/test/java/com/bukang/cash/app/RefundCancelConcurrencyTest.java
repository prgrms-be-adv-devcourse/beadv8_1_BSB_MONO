package com.bukang.cash.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import com.bukang.cash.config.CashWalletProperties;
import com.bukang.cash.domain.member.CashMember;
import com.bukang.cash.domain.payment.Payment;
import com.bukang.cash.domain.payment.PaymentStatus;
import com.bukang.cash.domain.wallet.LedgerRefType;
import com.bukang.cash.domain.wallet.Wallet;
import com.bukang.cash.out.CashMemberRepository;
import com.bukang.cash.out.PaymentRepository;
import com.bukang.cash.out.WalletLedgerRepository;
import com.bukang.cash.out.WalletRepository;

// PRO-86: 완료된 결제 하나에 환불(refund)과 취소(cancelPayment)가 동시에 오면, 둘 다 같은
// (customer, holding) 지갑 쌍을 lockCustomerAndHoldingWallets로 잠그므로 하나가 끝난 뒤에야
// 다른 하나가 시작된다. 먼저 끝난 쪽이 상태를 CANCELED/REFUNDED로 바꿔두면, 나중 쪽은 멱등
// 분기로 아무것도 더 안 바꾸고 끝나야 한다 (중복 환불 없음).
@SpringBootTest
@ActiveProfiles("test")
class RefundCancelConcurrencyTest {
	private static final int CUSTOMER_ID = 3;
	private static final int ORDER_ID = 500;
	private static final int AMOUNT = 10_000;

	@Autowired
	private CashFacade cashFacade;
	@Autowired
	private CashWalletProperties cashWalletProperties;
	@Autowired
	private CashMemberRepository cashMemberRepository;
	@Autowired
	private WalletRepository walletRepository;
	@Autowired
	private PaymentRepository paymentRepository;
	@Autowired
	private WalletLedgerRepository walletLedgerRepository;
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
	@DisplayName("PRO-86: 완료된 결제에 환불과 취소가 동시에 와도 한쪽만 반영되고, 돈은 정확히 한 번만 돌아온다")
	void concurrentRefundAndCancel_onlyOneTakesEffect() throws InterruptedException {
		createCustomerWallet(AMOUNT);
		createHoldingWallet();
		cashFacade.completeOrderPayment(ORDER_ID, CUSTOMER_ID, AMOUNT);
		assertBalance(CUSTOMER_ID, 0);
		assertBalance(cashWalletProperties.holdingMemberId(), AMOUNT);

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch startSignal = new CountDownLatch(1);
		Callable<Void> refundTask = () -> {
			startSignal.await();
			cashFacade.refund(ORDER_ID, CUSTOMER_ID);
			return null;
		};
		Callable<Void> cancelTask = () -> {
			startSignal.await();
			cashFacade.cancelPayment(ORDER_ID, CUSTOMER_ID);
			return null;
		};
		Future<Void> refundFuture = executor.submit(refundTask);
		Future<Void> cancelFuture = executor.submit(cancelTask);
		startSignal.countDown();

		Throwable refundError = awaitError(refundFuture);
		Throwable cancelError = awaitError(cancelFuture);
		executor.shutdown();

		System.out.println("PRO-86(환불/취소) refund 예외: " + summarize(refundError));
		System.out.println("PRO-86(환불/취소) cancel 예외: " + summarize(cancelError));

		assertThat(refundError).as("환불은 예외 없이 끝나야 한다(멱등 분기)").isNull();
		assertThat(cancelError).as("취소도 예외 없이 끝나야 한다(멱등 분기)").isNull();

		assertBalance(CUSTOMER_ID, AMOUNT);
		assertBalance(cashWalletProperties.holdingMemberId(), 0);
		Payment payment = paymentRepository.findByOrderId(ORDER_ID).orElseThrow();
		assertThat(payment.getStatus())
			.as("둘 중 하나로 확정되고, 두 번 반영되면 안 된다")
			.isIn(PaymentStatus.CANCELED, PaymentStatus.REFUNDED);
		assertThat(countLedgersForOrder(ORDER_ID))
			.as("결제 2줄 + 되돌림 2줄, 총 4줄만 있어야 한다 (중복 반영 시 6줄이 됨)")
			.isEqualTo(4);
	}

	private Throwable awaitError(Future<Void> future) throws InterruptedException {
		try {
			future.get(5, TimeUnit.SECONDS);
			return null;
		} catch (ExecutionException e) {
			return e.getCause();
		} catch (TimeoutException e) {
			return e;
		}
	}

	private String summarize(Throwable error) {
		return error == null ? "없음" : error.getClass().getSimpleName() + ": " + error.getMessage();
	}

	private long countLedgersForOrder(int orderId) {
		return walletLedgerRepository.findAll().stream()
			.filter(ledger -> ledger.getRefTypeCode() == LedgerRefType.Order && ledger.getRefId() == orderId)
			.count();
	}

	private void createCustomerWallet(int initialBalance) {
		createWallet(CUSTOMER_ID, initialBalance);
	}

	private void createHoldingWallet() {
		createWallet(cashWalletProperties.holdingMemberId(), 0);
	}

	private void createWallet(int memberId, int initialBalance) {
		transactionTemplate.executeWithoutResult(status -> {
			LocalDateTime now = LocalDateTime.of(2026, 10, 18, 10, 0);
			CashMember member = cashMemberRepository.save(new CashMember(memberId, now, now, "러너" + memberId));
			Wallet wallet = walletRepository.save(new Wallet(member));
			if (initialBalance != 0) {
				ReflectionTestUtils.setField(wallet, "balance", initialBalance);
				walletRepository.save(wallet);
			}
		});
	}

	private void assertBalance(int holderId, int expected) {
		assertThat(walletRepository.findById(holderId).orElseThrow().getBalance()).isEqualTo(expected);
	}
}
