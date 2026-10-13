package com.bukang.cash.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bukang.cash.domain.charge.WalletCharge;
import com.bukang.cash.domain.member.CashMember;
import com.bukang.cash.domain.wallet.LedgerRefType;
import com.bukang.cash.domain.wallet.LedgerType;
import com.bukang.cash.domain.wallet.Wallet;
import com.bukang.cash.domain.wallet.WalletLedger;
import com.bukang.cash.domain.withdrawal.WalletWithdrawal;

class WalletTest {
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 12, 10, 0);

	private Wallet newWallet() {
		return new Wallet(new CashMember(3, NOW, NOW, "러너3"));
	}

	@Test
	@DisplayName("지갑을 처음 만들면 잔액은 0원이고, 지갑 ID는 회원 ID와 같다")
	void createWallet() {
		Wallet wallet = newWallet();

		assertThat(wallet.getBalance()).isZero();
		assertThat(wallet.getId()).isEqualTo(3);
		assertThat(wallet.getHolder().getId()).isEqualTo(3);
	}

	@Test
	@DisplayName("credit은 잔액을 늘리고, 거래 후 잔액을 담은 원장을 돌려준다")
	void credit() {
		Wallet wallet = newWallet();

		WalletLedger ledger = wallet.credit(10_000, LedgerType.충전__PG결제_토스페이먼츠, LedgerRefType.WalletCharge, 7);

		assertThat(wallet.getBalance()).isEqualTo(10_000);
		assertThat(ledger.getWallet()).isSameAs(wallet);
		assertThat(ledger.getHolder()).isSameAs(wallet.getHolder());
		assertThat(ledger.getLedgerType()).isEqualTo(LedgerType.충전__PG결제_토스페이먼츠);
		assertThat(ledger.getAmount()).isEqualTo(10_000);
		assertThat(ledger.getBalanceAfter()).isEqualTo(10_000);
		assertThat(ledger.getRefTypeCode()).isEqualTo(LedgerRefType.WalletCharge);
		assertThat(ledger.getRefId()).isEqualTo(7);
	}

	@Test
	@DisplayName("debit은 잔액을 줄이고, 원장에는 음수 금액으로 남긴다")
	void debit() {
		Wallet wallet = newWallet();
		wallet.credit(10_000, LedgerType.충전__PG결제_토스페이먼츠, LedgerRefType.WalletCharge, 7);

		WalletLedger ledger = wallet.debit(3_000, LedgerType.사용__주문결제, LedgerRefType.Order, 15);

		assertThat(wallet.getBalance()).isEqualTo(7_000);
		assertThat(ledger.getAmount()).isEqualTo(-3_000);
		assertThat(ledger.getBalanceAfter()).isEqualTo(7_000);
		assertThat(ledger.getRefTypeCode()).isEqualTo(LedgerRefType.Order);
		assertThat(ledger.getRefId()).isEqualTo(15);
	}

	@Test
	@DisplayName("충전·환급 객체를 넘기면 ref 유형이 객체에 맞게 정해진다")
	void creditAndDebitWithEntity() {
		Wallet wallet = newWallet();
		WalletCharge charge = new WalletCharge(wallet, 10_000, "TOSS", "charge-key-1");
		WalletWithdrawal withdrawal = new WalletWithdrawal(wallet, 10_000, "004", "암호문", "러너3", "withdraw-key-1");

		WalletLedger chargeLedger = wallet.credit(10_000, LedgerType.충전__PG결제_토스페이먼츠, charge);
		WalletLedger withdrawalLedger = wallet.debit(10_000, LedgerType.출금__통장입금, withdrawal);

		assertThat(chargeLedger.getRefTypeCode()).isEqualTo(LedgerRefType.WalletCharge);
		assertThat(withdrawalLedger.getRefTypeCode()).isEqualTo(LedgerRefType.Withdrawal);
		assertThat(wallet.getBalance()).isZero();
	}
}
