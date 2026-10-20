package com.bukang.cash.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import com.bukang.cash.config.CashWalletProperties;
import com.bukang.cash.domain.member.CashMember;
import com.bukang.cash.domain.payout.PayoutStage;
import com.bukang.cash.domain.payout.PayoutTransferType;
import com.bukang.cash.domain.wallet.LedgerRefType;
import com.bukang.cash.domain.wallet.LedgerType;
import com.bukang.cash.domain.wallet.Wallet;
import com.bukang.cash.domain.wallet.WalletLedger;
import com.bukang.cash.dto.CashPayoutResponseDto;
import com.bukang.cash.out.CashMemberRepository;
import com.bukang.cash.out.PayoutTransferRepository;
import com.bukang.cash.out.WalletLedgerRepository;
import com.bukang.cash.out.WalletRepository;

// PRO-256: cash-service의 정산 지급 내부 API(POST /internal/v1/cash/payouts/{payoutId}) 유스케이스 테스트
@SpringBootTest
@ActiveProfiles("test")
class CashPayoutUseCaseTest {
	private static final int SELLER_ID = 10;

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
	private PayoutTransferRepository payoutTransferRepository;
	@Autowired
	private TransactionTemplate transactionTemplate;

	@AfterEach
	void tearDown() {
		payoutTransferRepository.deleteAll();
		walletLedgerRepository.deleteAll();
		walletRepository.deleteAll();
		cashMemberRepository.deleteAll();
	}

	@Test
	@DisplayName("1차 정산은 수수료 없이 대금 이동만 처리되고, 홀딩→판매자 2줄만 장부에 남는다")
	void firstStage_transfersSettlementOnly() {
		createHoldingWallet();
		createSystemWallet();
		createWallet(SELLER_ID, "판매자");

		CashPayoutResponseDto result = cashFacade.payout(500, SELLER_ID, PayoutStage.FIRST, 90_000, 0);

		assertThat(result.outcome()).isEqualTo(CashPayoutResponseDto.Outcome.PAID);
		assertThat(result.settlementAmount()).isEqualTo(90_000);
		assertThat(result.feeAmount()).isZero();
		assertBalance(cashWalletProperties.holdingMemberId(), -90_000);
		assertBalance(SELLER_ID, 90_000);
		assertBalance(cashWalletProperties.systemMemberId(), 0);
		assertThat(ledgersFor(PayoutTransferType.SETTLEMENT)).hasSize(2);
		assertThat(ledgersFor(PayoutTransferType.FEE)).isEmpty();
	}

	@Test
	@DisplayName("최종 정산은 대금(홀딩→판매자)과 수수료(홀딩→시스템)를 각각 2줄씩, 서로 다른 ref_id로 남긴다")
	void finalStage_transfersSettlementAndFeeWithDifferentRefIds() {
		createHoldingWallet();
		createSystemWallet();
		createWallet(SELLER_ID, "판매자");

		CashPayoutResponseDto result = cashFacade.payout(501, SELLER_ID, PayoutStage.FINAL, 90_000, 5_500);

		assertThat(result.outcome()).isEqualTo(CashPayoutResponseDto.Outcome.PAID);
		assertBalance(cashWalletProperties.holdingMemberId(), -95_500);
		assertBalance(SELLER_ID, 90_000);
		assertBalance(cashWalletProperties.systemMemberId(), 5_500);

		List<WalletLedger> settlementLedgers = ledgersFor(PayoutTransferType.SETTLEMENT);
		List<WalletLedger> feeLedgers = ledgersFor(PayoutTransferType.FEE);
		assertThat(settlementLedgers).hasSize(2);
		assertThat(feeLedgers).hasSize(2);
		int settlementRefId = settlementLedgers.get(0).getRefId();
		int feeRefId = feeLedgers.get(0).getRefId();
		assertThat(settlementLedgers).as("대금 이동 2줄은 같은 ref_id를 가리킨다")
			.allMatch(ledger -> ledger.getRefId() == settlementRefId);
		assertThat(feeLedgers).as("수수료 이동 2줄은 같은 ref_id를 가리킨다")
			.allMatch(ledger -> ledger.getRefId() == feeRefId);
		assertThat(settlementRefId).as("대금과 수수료는 ref_id가 달라야 한다").isNotEqualTo(feeRefId);
	}

	@Test
	@DisplayName("같은 정산 ID로 다시 요청해도 지갑 잔액이 한 번만 바뀌고 ALREADY_PAID를 돌려준다")
	void sameRequestTwice_changesBalanceOnlyOnce() {
		createHoldingWallet();
		createSystemWallet();
		createWallet(SELLER_ID, "판매자");

		CashPayoutResponseDto first = cashFacade.payout(502, SELLER_ID, PayoutStage.FINAL, 90_000, 5_500);
		CashPayoutResponseDto second = cashFacade.payout(502, SELLER_ID, PayoutStage.FINAL, 90_000, 5_500);

		assertThat(first.outcome()).isEqualTo(CashPayoutResponseDto.Outcome.PAID);
		assertThat(second.outcome()).isEqualTo(CashPayoutResponseDto.Outcome.ALREADY_PAID);
		assertBalance(cashWalletProperties.holdingMemberId(), -95_500);
		assertBalance(SELLER_ID, 90_000);
		assertBalance(cashWalletProperties.systemMemberId(), 5_500);
		assertThat(walletLedgerRepository.findAll()).as("장부는 대금 2줄 + 수수료 2줄만 있어야 한다").hasSize(4);
		assertThat(payoutTransferRepository.count()).isEqualTo(2);
	}

	@Test
	@DisplayName("최종 정산에서 상계로 대금이 0원이면 수수료만 옮기고, 판매자 지갑이 없어도 성공한다")
	void finalStage_settlementZero_transfersFeeOnlyWithoutSellerWallet() {
		createHoldingWallet();
		createSystemWallet();
		// 판매자 지갑을 일부러 만들지 않는다: 대금이 0원이면 판매자 지갑을 아예 건드리지 않아야 한다

		CashPayoutResponseDto result = cashFacade.payout(503, SELLER_ID, PayoutStage.FINAL, 0, 5_500);

		assertThat(result.outcome()).isEqualTo(CashPayoutResponseDto.Outcome.PAID);
		assertThat(result.settlementAmount()).isZero();
		assertThat(result.feeAmount()).isEqualTo(5_500);
		assertBalance(cashWalletProperties.holdingMemberId(), -5_500);
		assertBalance(cashWalletProperties.systemMemberId(), 5_500);
		assertThat(walletRepository.existsById(SELLER_ID)).as("판매자 지갑은 건드리지 않아 생기지 않아야 한다").isFalse();
		assertThat(ledgersFor(PayoutTransferType.SETTLEMENT)).as("대금이 0원이면 대금 이동 줄을 만들지 않는다").isEmpty();
		assertThat(ledgersFor(PayoutTransferType.FEE)).hasSize(2);
	}

	private List<WalletLedger> ledgersFor(PayoutTransferType transferType) {
		LedgerType creditType = transferType == PayoutTransferType.SETTLEMENT
			? LedgerType.정산수령__상품판매_대금
			: LedgerType.정산수령__상품판매_수수료;
		LedgerType debitType = transferType == PayoutTransferType.SETTLEMENT
			? LedgerType.정산지급__상품판매_대금
			: LedgerType.정산지급__상품판매_수수료;
		return walletLedgerRepository.findAll().stream()
			.filter(ledger -> ledger.getRefTypeCode() == LedgerRefType.Payout
				&& (ledger.getLedgerType() == creditType || ledger.getLedgerType() == debitType))
			.toList();
	}

	private void assertBalance(int holderId, int expectedDelta) {
		assertThat(walletRepository.findById(holderId).orElseThrow().getBalance()).isEqualTo(expectedDelta);
	}

	private void createHoldingWallet() {
		createWallet(cashWalletProperties.holdingMemberId(), "홀딩");
	}

	private void createSystemWallet() {
		createWallet(cashWalletProperties.systemMemberId(), "시스템");
	}

	private void createWallet(int memberId, String nickname) {
		transactionTemplate.executeWithoutResult(status -> {
			LocalDateTime now = LocalDateTime.of(2026, 10, 20, 10, 0);
			CashMember member = cashMemberRepository.save(new CashMember(memberId, now, now, nickname));
			walletRepository.save(new Wallet(member));
		});
	}
}
