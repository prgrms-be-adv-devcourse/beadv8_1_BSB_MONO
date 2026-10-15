package com.bukang.payout.domain;

import static jakarta.persistence.EnumType.STRING;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

import com.bukang.common.global.jpa.entity.BaseIdAndTime;
import com.bukang.payout.domain.exception.PayoutRecoveryConflictException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회수: 이미 지급한 정산금 중 판매자가 플랫폼에 돌려줄 돈 (payout.md 4-2, PRO-33)
 * 대회 취소는 대회당 1건이고, 금액은 그 대회에 지급 완료된 정산 내역의 지급액 합이다 (참가권이 모두 환불되므로 준 돈 전부)
 * 판매자가 직접 입금하고 관리자가 입금을 확인해 끝낸다. 구매자 환불은 회수를 기다리지 않는다 (10/15 [제안])
 * 판매자, 대회는 다른 도메인이라 연관관계 없이 ID만 둔다
 */
@Entity
@NoArgsConstructor(access = PROTECTED)
@Getter
@Table(name = "PAYOUT_PAYOUT_RECOVERY")
public class PayoutRecovery extends BaseIdAndTime {
	@Column(nullable = false)
	private int sellerId;

	@Column(nullable = false)
	private int raceId;

	@Enumerated(STRING)
	@Column(nullable = false, length = 30)
	private PayoutRecoveryReason reasonType;

	@Column(nullable = false)
	private long amount;

	@Enumerated(STRING)
	@Column(nullable = false, length = 20)
	private PayoutRecoveryStatus status;

	@Column(nullable = false)
	private LocalDateTime requestedAt;

	private LocalDateTime completedAt;

	// 입금을 확인한 관리자 회원 ID. 돈이 들어온 것을 누가 확인했는지 남긴다. 요청 중에는 NULL
	private Integer completedBy;

	public PayoutRecovery(int sellerId, int raceId, PayoutRecoveryReason reasonType, long amount,
		LocalDateTime requestedAt) {
		if (amount <= 0) {
			throw new IllegalArgumentException("회수액은 0보다 커야 합니다. amount=%d".formatted(amount));
		}
		this.sellerId = sellerId;
		this.raceId = raceId;
		this.reasonType = reasonType;
		this.amount = amount;
		this.requestedAt = requestedAt;
		this.status = PayoutRecoveryStatus.REQUESTED;
	}

	// 관리자가 판매자의 입금을 확인했다
	public void complete(int completedBy, LocalDateTime completedAt) {
		if (status == PayoutRecoveryStatus.COMPLETED) {
			throw new PayoutRecoveryConflictException("이미 완료된 회수입니다.");
		}
		this.status = PayoutRecoveryStatus.COMPLETED;
		this.completedBy = completedBy;
		this.completedAt = completedAt;
	}
}
