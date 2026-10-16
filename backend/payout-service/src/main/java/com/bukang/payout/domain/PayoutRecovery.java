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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회수: 판매자에게 주었거나 주기로 한 선금 중 환불된 참가권 몫을 돌려받는 절차 (payout.md 4-2, PRO-33)
 * 돈의 기록은 원장 줄(요청 · 상계 · 입금)이 맡고, 이 엔티티는 절차(요청 · 완료 상태, 완료 방식)만 맡는다 (PRO-214)
 * - 대회 취소: 대회당 1건, 금액은 그때 남은 선금(ADVANCE 잔고)
 * - 일정 변경: 환불된 참가권마다 1건, 금액은 그 참가권의 1차 몫(가격 × 40%, 원 미만 버림)
 * 같은 판매자 × 대회의 갚을 돈(RECEIVABLE)이 0이 될 때(상계 또는 입금) 그 대회의 요청 중인 회수를 함께 완료한다
 * 회수가 끝나야 구매자 환불이 나간다 (정책 원문)
 * 판매자, 대회, 주문 상품은 다른 도메인이라 연관관계 없이 ID만 둔다
 */
@Entity
@NoArgsConstructor(access = PROTECTED)
@Getter
// 대회 취소 회수는 대회당 1건이다. 같은 메시지를 두 곳이 동시에 처리해 확인을 함께 통과해도 DB가 두 번째 저장을 막는다
// 그 처리는 예외로 롤백되고 재시도 때 이미 만든 회수를 보고 건너뛴다
@Table(name = "PAYOUT_PAYOUT_RECOVERY", uniqueConstraints = {
	@UniqueConstraint(name = "uk_payout_recovery_race_reason", columnNames = {"race_id", "reason_type"})
})
public class PayoutRecovery extends BaseIdAndTime {
	@Column(nullable = false)
	private int sellerId;

	@Column(nullable = false)
	private int raceId;

	// 일정 변경 회수의 환불된 참가권(주문 상품). 같은 참가권으로 회수가 두 번 생기지 않게 유일하다. 대회 취소는 NULL
	@Column(unique = true)
	private Integer orderItemId;

	@Enumerated(STRING)
	@Column(nullable = false, length = 30)
	private PayoutRecoveryReason reasonType;

	// 요청한 금액. 화면 표시용이고 금액 계산은 원장 줄로 한다
	@Column(nullable = false)
	private long amount;

	@Enumerated(STRING)
	@Column(nullable = false, length = 20)
	private PayoutRecoveryStatus status;

	@Column(nullable = false)
	private LocalDateTime requestedAt;

	private LocalDateTime completedAt;

	// 입금을 확인한 회원 ID. 상계로 끝나면 시스템(0). 요청 중에는 NULL
	private Integer completedBy;

	// 어떻게 끝났는지 (입금 / 상계). 요청 중에는 NULL
	@Enumerated(STRING)
	@Column(length = 20)
	private PayoutRecoveryCompletion completion;

	public PayoutRecovery(int sellerId, int raceId, PayoutRecoveryReason reasonType, long amount,
		LocalDateTime requestedAt) {
		this(sellerId, raceId, null, reasonType, amount, requestedAt);
	}

	private PayoutRecovery(int sellerId, int raceId, Integer orderItemId, PayoutRecoveryReason reasonType, long amount,
		LocalDateTime requestedAt) {
		if (amount <= 0) {
			throw new IllegalArgumentException("회수액은 0보다 커야 합니다. amount=%d".formatted(amount));
		}
		this.sellerId = sellerId;
		this.raceId = raceId;
		this.orderItemId = orderItemId;
		this.reasonType = reasonType;
		this.amount = amount;
		this.requestedAt = requestedAt;
		this.status = PayoutRecoveryStatus.REQUESTED;
	}

	// 일정 변경으로 환불된 참가권 한 장의 회수
	public static PayoutRecovery scheduleChanged(int sellerId, int raceId, int orderItemId, long amount,
		LocalDateTime requestedAt) {
		return new PayoutRecovery(sellerId, raceId, orderItemId, PayoutRecoveryReason.SCHEDULE_CHANGED, amount,
			requestedAt);
	}

	// 판매자의 입금을 확인했다
	public void complete(int completedBy, LocalDateTime completedAt) {
		finish(PayoutRecoveryCompletion.DEPOSIT, completedBy, completedAt);
	}

	// 송금 전 정산 묶음에서 빼서 갚을 돈이 0이 됐다
	public void completeByOffset(LocalDateTime completedAt) {
		finish(PayoutRecoveryCompletion.OFFSET, PayoutHold.SYSTEM_MEMBER_ID, completedAt);
	}

	private void finish(PayoutRecoveryCompletion completion, int completedBy, LocalDateTime completedAt) {
		if (status == PayoutRecoveryStatus.COMPLETED) {
			throw new PayoutRecoveryConflictException("이미 완료된 회수입니다.");
		}
		this.status = PayoutRecoveryStatus.COMPLETED;
		this.completion = completion;
		this.completedBy = completedBy;
		this.completedAt = completedAt;
	}
}
