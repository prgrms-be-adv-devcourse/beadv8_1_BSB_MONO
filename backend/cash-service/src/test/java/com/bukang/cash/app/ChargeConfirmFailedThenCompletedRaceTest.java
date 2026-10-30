package com.bukang.cash.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import com.bukang.cash.domain.charge.ChargeStatus;
import com.bukang.cash.domain.charge.WalletCharge;
import com.bukang.cash.domain.exception.PaymentApprovalFailedException;
import com.bukang.cash.domain.member.CashMember;
import com.bukang.cash.domain.wallet.Wallet;
import com.bukang.cash.dto.CashChargeResponseDto;
import com.bukang.cash.out.CashMemberRepository;
import com.bukang.cash.out.WalletChargeRepository;
import com.bukang.cash.out.WalletLedgerRepository;
import com.bukang.cash.out.WalletRepository;
import com.bukang.cash.out.pg.PaymentApprovalResult;
import com.bukang.cash.out.pg.PaymentClient;

// PR 리뷰 지적: 같은 orderId로 동시에 두 confirm 요청이 오면(새로고침·중복 클릭) 둘 다 Toss까지 간다.
// 뒤처진 요청이 먼저 거절당해 FAILED를 저장해버린 뒤, 실제로 승인된 다른 요청이 그 FAILED 기록을
// 그대로 믿고 돌려주면 "돈은 나갔는데 FAILED로 기록되는" money-lost가 된다. 래치로 순서를 고정해
// 정확히 이 순서(진 쪽 FAILED 커밋 → 이긴 쪽 성공 처리)를 재현한다.
@SpringBootTest
@ActiveProfiles("test")
class ChargeConfirmFailedThenCompletedRaceTest {
	private static final long CUSTOMER_ID = 3;

	@Autowired
	private CashFacade cashFacade;
	@Autowired
	private CashMemberRepository cashMemberRepository;
	@Autowired
	private WalletRepository walletRepository;
	@Autowired
	private WalletChargeRepository walletChargeRepository;
	@Autowired
	private WalletLedgerRepository walletLedgerRepository;
	@Autowired
	private TransactionTemplate transactionTemplate;

	@MockitoBean
	private PaymentClient paymentClient;

	@AfterEach
	void tearDown() {
		walletChargeRepository.deleteAll();
		walletLedgerRepository.deleteAll();
		walletRepository.deleteAll();
		cashMemberRepository.deleteAll();
	}

	@Test
	@DisplayName("같은 orderId 동시 confirm 중 한쪽이 먼저 FAILED를 저장해도, "
		+ "실제로 승인된 다른 쪽 결과가 묻히지 않고 COMPLETED로 반영된다")
	void losingFailedRecordDoesNotSwallowWinningApproval() throws Exception {
		createCustomerWallet();
		CountDownLatch aReachedToss = new CountDownLatch(1);
		CountDownLatch bFailedCommitted = new CountDownLatch(1);

		// A: Toss 호출에 들어서면 신호를 보내고, B가 FAILED를 커밋할 때까지 기다렸다가 승인을 돌려준다
		given(paymentClient.approve(eq("pay_a"), eq("order_1"), anyLong())).willAnswer(invocation -> {
			aReachedToss.countDown();
			bFailedCommitted.await(5, TimeUnit.SECONDS);
			return new PaymentApprovalResult(10_000, "toss_key_a");
		});
		// B: 즉시 (money-lost와 무관한 임의의 사유로) 거절
		given(paymentClient.approve(eq("pay_b"), eq("order_1"), anyLong()))
			.willThrow(new PaymentApprovalFailedException("한도를 초과했습니다."));

		ExecutorService executor = Executors.newFixedThreadPool(2);
		Future<CashChargeResponseDto> aFuture = executor.submit(() ->
			cashFacade.confirmWalletCharge(CUSTOMER_ID, "pay_a", "order_1", 10_000));

		assertThat(aReachedToss.await(5, TimeUnit.SECONDS))
			.as("A가 Toss 호출 지점까지 가야 B를 시작한다")
			.isTrue();

		Future<CashChargeResponseDto> bFuture = executor.submit(() ->
			cashFacade.confirmWalletCharge(CUSTOMER_ID, "pay_b", "order_1", 10_000));
		CashChargeResponseDto bResponse = bFuture.get(5, TimeUnit.SECONDS);
		assertThat(bResponse.outcome())
			.as("B는 거절당해 FAILED를 먼저 커밋해야 한다")
			.isEqualTo(CashChargeResponseDto.Outcome.FAILED);

		bFailedCommitted.countDown();
		CashChargeResponseDto aResponse = aFuture.get(5, TimeUnit.SECONDS);
		executor.shutdown();

		assertThat(aResponse.outcome())
			.as("A는 실제로 Toss 승인을 받았으니, B가 먼저 남긴 FAILED에 묻히지 않고 COMPLETED여야 한다")
			.isEqualTo(CashChargeResponseDto.Outcome.COMPLETED);
		assertBalance(CUSTOMER_ID, 10_000);
		WalletCharge charge = walletChargeRepository.findByIdempotencyKey("order_1").orElseThrow();
		assertThat(charge.getStatus()).isEqualTo(ChargeStatus.COMPLETED);
		assertThat(charge.getPgPaymentKey()).isEqualTo("toss_key_a");
		assertThat(walletChargeRepository.count()).as("행은 하나만 있어야 한다(재사용, 새로 안 만듦)").isEqualTo(1);
		assertThat(walletLedgerRepository.findAll()).as("적립 장부도 한 줄만 남아야 한다").hasSize(1);
	}

	private void createCustomerWallet() {
		transactionTemplate.executeWithoutResult(status -> {
			LocalDateTime now = LocalDateTime.of(2026, 10, 19, 10, 0);
			CashMember member = cashMemberRepository.save(new CashMember(CUSTOMER_ID, now, now, "러너" + CUSTOMER_ID));
			walletRepository.save(new Wallet(member));
		});
	}

	private void assertBalance(long holderId, int expected) {
		assertThat(walletRepository.findById(holderId).orElseThrow().getBalance()).isEqualTo(expected);
	}
}
