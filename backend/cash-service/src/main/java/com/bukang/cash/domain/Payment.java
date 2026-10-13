package com.bukang.cash.domain;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;

import java.time.LocalDateTime;

import com.bukang.common.global.jpa.entity.BaseIdAndTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
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
	// 주문은 다른 서비스의 테이블이라 연관관계(FK) 없이 ID 값으로만 보관한다
	@Column(nullable = false)
	private int orderId;

	@ManyToOne(fetch = LAZY, optional = false)
	private Wallet wallet;

	private long amount;

	@Enumerated(STRING)
	@Column(nullable = false, length = 20)
	private PaymentStatus status;
	@Enumerated(STRING)
	@Column(length = 50) // status가 FAILED일 때만 채운다
	private PaymentFailReason failReason;

	@Column(nullable = false, unique = true, length = 64) // 중복 결제 방지
	private String idempotencyKey;
	private LocalDateTime completedAt;

	public Payment(int orderId, Wallet wallet, long amount, String idempotencyKey) {
		this.orderId = orderId;
		this.wallet = wallet;
		this.amount = amount;
		this.idempotencyKey = idempotencyKey;
		this.status = PaymentStatus.PENDING;
	}
}
