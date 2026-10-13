package com.bukang.cash.domain.withdrawal;

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
 * 예치금 환급(현금 환불) 신청
 * 신청 일시는 createDate로 대신한다. 금액은 신청 시점 잔액 전부다.
 */
@Entity
@NoArgsConstructor
@Getter
@Table(name = "CASH_WALLET_WITHDRAWAL")
public class WalletWithdrawal extends BaseIdAndTime {
	@ManyToOne(fetch = LAZY, optional = false)
	private Wallet wallet;
	private long amount;

	@Column(nullable = false, length = 10)
	private String bankCode;
	@Column(nullable = false, length = 255) // 암호문을 저장한다
	private String accountNumber;
	@Column(nullable = false, length = 50)
	private String accountHolder;

	@Enumerated(STRING)
	@Column(nullable = false, length = 20)
	private WithdrawalStatus status;

	@Enumerated(STRING)
	@Column(length = 50) // status가 FAILED일 때만 채운다
	private WithdrawalFailReason failReason;

	@Column(nullable = false, unique = true, length = 64) // 중복 신청 방지
	private String idempotencyKey;
	private LocalDateTime completedAt;

	public WalletWithdrawal(
		Wallet wallet,
		long amount,
		String bankCode,
		String accountNumber,
		String accountHolder,
		String idempotencyKey
	) {
		this.wallet = wallet;
		this.amount = amount;
		this.bankCode = bankCode;
		this.accountNumber = accountNumber;
		this.accountHolder = accountHolder;
		this.idempotencyKey = idempotencyKey;
		this.status = WithdrawalStatus.REQUESTED;
	}
}
