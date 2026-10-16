package com.bukang.cash.domain.charge;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;

import java.time.LocalDateTime;

import com.bukang.cash.domain.wallet.Wallet;
import com.bukang.common.global.jpa.entity.BaseIdAndTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * PG사를 통한 예치금 충전 요청·승인 내역
 * 요청 일시는 createDate로 대신한다.
 */
@Entity
@NoArgsConstructor
@Getter
@Table(name = "CASH_WALLET_CHARGE")
public class WalletCharge extends BaseIdAndTime {
	@ManyToOne(fetch = LAZY, optional = false)
	private Wallet wallet;

	private int amount;

	@Column(nullable = false, length = 30)
	private String pgProvider;

	@Column(length = 255)
	private String pgPaymentKey;
	@Column(nullable = false, unique = true, length = 64) // 중복 충전 방지
	private String idempotencyKey;

	@Enumerated(STRING)
	@Column(nullable = false, length = 20)
	private ChargeStatus status;
	private LocalDateTime chargeDate;

	public WalletCharge(Wallet wallet, int amount, String pgProvider, String idempotencyKey) {
		this.wallet = wallet;
		this.amount = amount;
		this.pgProvider = pgProvider;
		this.idempotencyKey = idempotencyKey;
		this.status = ChargeStatus.REQUESTED;
	}
	public void markCompleted(String pgPaymentKey, LocalDateTime chargeDate) {
		this.pgPaymentKey = pgPaymentKey;
		this.status = ChargeStatus.COMPLETED;
		this.chargeDate = chargeDate;
	}

	public void markFailed() {
		this.status = ChargeStatus.FAILED;
	}
}
