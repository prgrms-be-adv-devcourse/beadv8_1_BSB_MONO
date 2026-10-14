package com.bukang.cash.domain.payment;

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
 * 예치금으로 하는 주문 결제
 * 시도(요청) 일시는 createDate로 대신한다.
 */
@Entity
@NoArgsConstructor
@Getter
@Table(name = "CASH_PAYMENT")
public class Payment extends BaseIdAndTime {
	// 주문은 다른 서비스의 테이블이라 연관관계(FK) 없이 ID 값으로만 보관한다, 주문 id를 멱등 키로 사용
	@Column(nullable = false, unique = true)
	private int orderId;

	@ManyToOne(fetch = LAZY, optional = false)
	private Wallet wallet;

	private int amount;

	@Enumerated(STRING)
	@Column(nullable = false, length = 20)
	private PaymentStatus status;
	@Enumerated(STRING)
	@Column(length = 50) // status가 FAILED일 때만 채운다
	private PaymentFailReason failReason;

	private LocalDateTime completedAt;

	public Payment(int orderId, Wallet wallet, int amount) {
		this.orderId = orderId;
		this.wallet = wallet;
		this.amount = amount;
		this.status = PaymentStatus.PENDING;
	}

	public void markCompleted(LocalDateTime completedAt) {
		this.status = PaymentStatus.COMPLETED;
		this.completedAt = completedAt;
		this.failReason = null;
	}

	public void markFailed(PaymentFailReason failReason) {
		this.status = PaymentStatus.FAILED;
		this.failReason = failReason;
	}

	public void markCanceled() {
		this.status = PaymentStatus.CANCELED;
		this.failReason = null;
	}

	public void markRefunded() {
		this.status = PaymentStatus.REFUNDED;
		this.failReason = null;
	}
}
