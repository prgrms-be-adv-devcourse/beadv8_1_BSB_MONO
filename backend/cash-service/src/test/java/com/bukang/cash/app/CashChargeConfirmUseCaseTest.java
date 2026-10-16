package com.bukang.cash.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import com.bukang.cash.domain.charge.ChargeStatus;
import com.bukang.cash.domain.charge.WalletCharge;
import com.bukang.cash.domain.exception.ChargeAmountTooSmallException;
import com.bukang.cash.domain.exception.ChargeMemberMismatchException;
import com.bukang.cash.domain.exception.PaymentApprovalFailedException;
import com.bukang.cash.domain.exception.WalletNotFoundException;
import com.bukang.cash.domain.member.CashMember;
import com.bukang.cash.domain.wallet.LedgerRefType;
import com.bukang.cash.domain.wallet.LedgerType;
import com.bukang.cash.domain.wallet.Wallet;
import com.bukang.cash.domain.wallet.WalletLedger;
import com.bukang.cash.dto.CashChargeResponseDto;
import com.bukang.cash.out.CashMemberRepository;
import com.bukang.cash.out.WalletChargeRepository;
import com.bukang.cash.out.WalletLedgerRepository;
import com.bukang.cash.out.WalletRepository;
import com.bukang.cash.out.pg.PaymentApprovalResult;
import com.bukang.cash.out.pg.PaymentClient;

// FakePaymentClient(mode=fake)는 항상 성공해서 실패·멱등 분기를 확인할 수 없으므로,
// PaymentClient 빈을 Mockito 목으로 바꿔서 승인 성공/실패를 직접 제어한다
@SpringBootTest
@ActiveProfiles("test")
class CashChargeConfirmUseCaseTest {
	private static final int CUSTOMER_ID = 3;

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
	@DisplayName("1만 원 미만이면 PG 호출 없이 거절하고, 충전 행도 만들지 않는다")
	void rejectsBelowMinimumAmount() {
		createCustomerWallet(0);

		assertThatThrownBy(() -> cashFacade.confirmWalletCharge(CUSTOMER_ID, "pay_1", "order_1", 9_999))
			.isInstanceOf(ChargeAmountTooSmallException.class);

		assertThat(walletChargeRepository.findByIdempotencyKey("order_1")).isEmpty();
		assertBalance(CUSTOMER_ID, 0);
		verify(paymentClient, never()).approve(anyString(), anyString(), anyInt());
	}

	@Test
	@DisplayName("승인 성공 시 PG 응답의 실제 승인 금액으로 지갑에 반영한다 (요청 금액을 그대로 믿지 않는다)")
	void approvedAmountFromPgIsUsedNotRequestedAmount() {
		createCustomerWallet(0);
		given(paymentClient.approve("pay_1", "order_1", 10_000))
			.willReturn(new PaymentApprovalResult(15_000, "toss_key_1"));

		CashChargeResponseDto response = cashFacade.confirmWalletCharge(CUSTOMER_ID, "pay_1", "order_1", 10_000);

		assertThat(response.outcome()).isEqualTo(CashChargeResponseDto.Outcome.COMPLETED);
		assertThat(response.balance()).isEqualTo(15_000);
		assertBalance(CUSTOMER_ID, 15_000);

		WalletCharge charge = walletChargeRepository.findByIdempotencyKey("order_1").orElseThrow();
		assertThat(charge.getStatus()).isEqualTo(ChargeStatus.COMPLETED);
		assertThat(charge.getPgPaymentKey()).isEqualTo("toss_key_1");
	}

	@Test
	@DisplayName("승인 성공 시 장부 1줄(충전__PG결제_토스페이먼츠)이 남는다")
	void approvedChargeLeavesOneLedgerEntry() {
		createCustomerWallet(0);
		given(paymentClient.approve(anyString(), anyString(), anyInt()))
			.willReturn(new PaymentApprovalResult(10_000, "toss_key_1"));

		cashFacade.confirmWalletCharge(CUSTOMER_ID, "pay_1", "order_1", 10_000);

		assertThat(walletLedgerRepository.findAll()).hasSize(1);
		WalletLedger ledger = walletLedgerRepository.findAll().get(0);
		assertThat(ledger.getLedgerType()).isEqualTo(LedgerType.충전__PG결제_토스페이먼츠);
		assertThat(ledger.getAmount()).isEqualTo(10_000);
		assertThat(ledger.getRefTypeCode()).isEqualTo(LedgerRefType.WalletCharge);
	}

	@Test
	@DisplayName("PG 승인이 거절되면 예외 대신 실패 행과 200 실패 응답을 남기고, 잔액·장부는 그대로다")
	void pgApprovalFailureMarksFailedAndReturnsFailedResponse() {
		createCustomerWallet(0);
		given(paymentClient.approve(anyString(), anyString(), anyInt()))
			.willThrow(new PaymentApprovalFailedException("PG사가 결제 승인을 거절했습니다."));

		CashChargeResponseDto response = cashFacade.confirmWalletCharge(CUSTOMER_ID, "pay_1", "order_1", 10_000);

		assertThat(response.outcome()).isEqualTo(CashChargeResponseDto.Outcome.FAILED);
		assertThat(response.balance()).isEqualTo(0);
		assertBalance(CUSTOMER_ID, 0);

		WalletCharge charge = walletChargeRepository.findByIdempotencyKey("order_1").orElseThrow();
		assertThat(charge.getStatus()).isEqualTo(ChargeStatus.FAILED);
		assertThat(walletLedgerRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("이미 COMPLETED인 orderId로 다시 요청하면 PG를 다시 부르지 않고 같은 결과를 돌려준다")
	void alreadyCompletedDoesNotCallPgAgain() {
		Wallet wallet = createCustomerWallet(10_000);
		WalletCharge charge = walletChargeRepository.save(new WalletCharge(wallet, 10_000, "TOSS", "order_1"));
		ReflectionTestUtils.setField(charge, "status", ChargeStatus.COMPLETED);
		walletChargeRepository.saveAndFlush(charge);

		CashChargeResponseDto response = cashFacade.confirmWalletCharge(CUSTOMER_ID, "pay_1", "order_1", 10_000);

		assertThat(response.outcome()).isEqualTo(CashChargeResponseDto.Outcome.COMPLETED);
		assertThat(response.balance()).isEqualTo(10_000);
		assertBalance(CUSTOMER_ID, 10_000);
		verify(paymentClient, never()).approve(anyString(), anyString(), anyInt());
	}

	@Test
	@DisplayName("이미 FAILED인 orderId로 다시 요청해도 PG를 다시 부르지 않고 FAILED 그대로 돌려준다")
	void alreadyFailedDoesNotRetry() {
		Wallet wallet = createCustomerWallet(5_000);
		WalletCharge charge = walletChargeRepository.save(new WalletCharge(wallet, 10_000, "TOSS", "order_1"));
		ReflectionTestUtils.setField(charge, "status", ChargeStatus.FAILED);
		walletChargeRepository.saveAndFlush(charge);

		CashChargeResponseDto response = cashFacade.confirmWalletCharge(CUSTOMER_ID, "pay_1", "order_1", 10_000);

		assertThat(response.outcome()).isEqualTo(CashChargeResponseDto.Outcome.FAILED);
		assertBalance(CUSTOMER_ID, 5_000);
		verify(paymentClient, never()).approve(anyString(), anyString(), anyInt());
	}

	@Test
	@DisplayName("다른 회원의 orderId로 요청하면 거절하고 PG를 부르지 않는다")
	void rejectsWhenOrderIdBelongsToAnotherMember() {
		int otherMemberId = 4;
		Wallet otherWallet = createWallet(otherMemberId, 10_000);
		WalletCharge otherCharge = walletChargeRepository.save(
			new WalletCharge(otherWallet, 10_000, "TOSS", "order_1"));
		ReflectionTestUtils.setField(otherCharge, "status", ChargeStatus.COMPLETED);
		walletChargeRepository.saveAndFlush(otherCharge);

		createCustomerWallet(0);

		assertThatThrownBy(() -> cashFacade.confirmWalletCharge(CUSTOMER_ID, "pay_1", "order_1", 10_000))
			.isInstanceOf(ChargeMemberMismatchException.class);

		assertBalance(otherMemberId, 10_000);
		assertBalance(CUSTOMER_ID, 0);
		verify(paymentClient, never()).approve(anyString(), anyString(), anyInt());
	}

	@Test
	@DisplayName("지갑이 없으면 404 예외로 알린다")
	void walletNotFound() {
		assertThatThrownBy(() -> cashFacade.confirmWalletCharge(999, "pay_1", "order_1", 10_000))
			.isInstanceOf(WalletNotFoundException.class);
	}

	private Wallet createCustomerWallet(int initialBalance) {
		return createWallet(CUSTOMER_ID, initialBalance);
	}

	private Wallet createWallet(int memberId, int initialBalance) {
		return transactionTemplate.execute(status -> {
			LocalDateTime now = LocalDateTime.of(2026, 10, 15, 10, 0);
			CashMember member = cashMemberRepository.save(new CashMember(memberId, now, now, "러너" + memberId));
			Wallet wallet = walletRepository.save(new Wallet(member));
			if (initialBalance != 0) {
				walletLedgerRepository.save(
					wallet.credit(initialBalance, LedgerType.충전__PG결제_토스페이먼츠, LedgerRefType.WalletCharge, memberId));
			}
			return wallet;
		});
	}

	private void assertBalance(int holderId, int expected) {
		assertThat(walletRepository.findById(holderId).orElseThrow().getBalance()).isEqualTo(expected);
	}
}
