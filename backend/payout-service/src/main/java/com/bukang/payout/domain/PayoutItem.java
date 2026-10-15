package com.bukang.payout.domain;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PROTECTED;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.bukang.common.global.jpa.entity.BaseIdAndTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 정산 항목: 정산 내역에 묶인 참가권 1장 (주문 상품 하나)
 * 추가만 하고 고치지 않는다. 그래서 setter와 변경 메서드를 두지 않는다
 */
@Entity
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PACKAGE)
@Getter
@Table(name = "PAYOUT_PAYOUT_ITEM")
public class PayoutItem extends BaseIdAndTime {
	@ManyToOne(fetch = LAZY, optional = false)
	@JoinColumn(name = "payout_id", nullable = false)
	private Payout payout;

	// 주문 도메인의 주문 상품 ID
	@Column(nullable = false)
	private int orderItemId;

	@Enumerated(STRING)
	@Column(nullable = false, length = 20)
	private PayoutRecordType recordType;

	// 판매는 +, 환불은 -
	@Column(nullable = false)
	private long amount;

	// 기록 시점의 수수료율 (예: 0.0550). 나중에 수수료율이 바뀌어도 이 항목은 그대로다
	@Column(nullable = false, precision = 5, scale = 4)
	private BigDecimal feeRate;

	// 정산 대상일: 대회 다음 날
	private LocalDate targetDate;

	// 이 참가권 1장의 수수료: 금액 × 수수료율, 원 미만 버림. 정산 내역의 수수료는 이 값의 합이다 (payout.md 2-1)
	// 칸으로 저장하지 않고 계산한다. 금액과 수수료율이 바뀌지 않으므로 언제 계산해도 같다
	// 버림은 0 쪽(DOWN)이다. 음수 항목(환불)에서 FLOOR는 1원을 더 빼므로 쓰지 않는다 (PRO-114)
	public long getFeeAmount() {
		return TwoStagePayoutCalculator.rate(amount, feeRate);
	}
}
