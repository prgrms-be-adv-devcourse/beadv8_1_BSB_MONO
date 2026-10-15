package com.bukang.payout.domain;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static lombok.AccessLevel.PROTECTED;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.bukang.common.global.jpa.entity.BaseIdAndTime;
import com.bukang.payout.domain.exception.PayoutHoldConflictException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 정산 내역: 판매자 × 대회 × 월로 한 번에 지급하는 묶음
 * 판매자, 대회는 다른 도메인이라 연관관계 없이 ID만 둔다
 */
@Entity
@NoArgsConstructor(access = PROTECTED)
@Getter
// 정산 생성 배치가 두 번 돌아도 같은 묶음이 두 번 지급되지 않도록 DB에서 막는다
@Table(name = "PAYOUT_PAYOUT", uniqueConstraints = {
	@UniqueConstraint(columnNames = {"seller_id", "race_id", "payout_month"})
})
public class Payout extends BaseIdAndTime {
	private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
	// 정기 지급일: 정산 월의 다음 달 20일 (휴일이면 다음 영업일, payout.md 2-4)
	private static final int PAY_DAY_OF_MONTH = 20;

	// 정산 ID: ST-정산 내역을 만든 날-16진수 4자리 (예: ST-20261029-0A3F)
	// 끝 4자리를 PK에서 만들어 저장 뒤에 채우므로 NULL을 허용한다
	@Column(unique = true, length = 20)
	private String payoutCode;

	@Column(nullable = false)
	private int sellerId;

	@Column(nullable = false)
	private int raceId;

	// 정산 내역을 만들 때 상품 서비스에서 받은 대회 이름. 영수증처럼 정산한 때의 이름을 남긴다
	// 판매자 조회 때 상품 서비스를 부르지 않으려고 저장한다 (정산은 대회 다음 날 만들어져 그 뒤 이름이 바뀔 일은 드물다)
	@Column(nullable = false, length = 200)
	private String raceName;

	// YYYY-MM. 정산 대상일이 속한 달
	@Column(nullable = false, length = 7)
	private String payoutMonth;

	@Column(nullable = false)
	private long salesAmount;

	@Column(nullable = false)
	private long refundAmount;

	@Column(nullable = false)
	private long feeAmount;

	@Column(nullable = false)
	private long payoutAmount;

	@Enumerated(STRING)
	@Column(nullable = false, length = 20)
	private PayoutStatus status;

	@Column(nullable = false)
	private LocalDateTime settledAt;

	// 지급 예정일. 지급 배치는 이 날이 지난 CALCULATED만 지급한다
	// 보류를 풀 때(PRO-26) 해제일 뒤 돌아오는 지급일(20일)로 다시 정하므로, 배치는 보류 기록을 보지 않아도 된다
	@Column(nullable = false)
	private LocalDate scheduledPayDate;

	private LocalDateTime paidAt;

	// 낙관적 락: 지급 배치와 보류 API가 같은 정산 내역을 동시에 바꾸면 늦게 커밋한 쪽이 실패한다
	// 없으면 배치가 읽은 CALCULATED를 근거로, 그 사이 걸린 HELD를 PAID로 덮어쓸 수 있다
	@Version
	private long version;

	// 정산 항목은 추가만 한다. orphanRemoval을 켜면 리스트에서 빼는 것만으로 행이 지워지므로 쓰지 않는다
	@OneToMany(mappedBy = "payout", cascade = ALL)
	private List<PayoutItem> items = new ArrayList<>();

	// 상태 이력도 추가만 한다. 상태를 바꾸는 메서드가 changeStatus로 한 줄씩 남겨 빠뜨릴 수 없게 한다 (PRO-34)
	// LAZY라 상태를 바꿀 때 이력 목록을 한 번 읽는다. 그래서 상태 변경은 트랜잭션(세션) 안에서만 한다
	@OneToMany(mappedBy = "payout", cascade = ALL)
	private List<PayoutStatusHistory> statusHistories = new ArrayList<>();

	public Payout(int sellerId, int raceId, String raceName, YearMonth payoutMonth, LocalDateTime settledAt,
		BusinessDayCalendar calendar) {
		this.sellerId = sellerId;
		this.raceId = raceId;
		this.raceName = raceName;
		this.payoutMonth = payoutMonth.toString();
		this.settledAt = settledAt;
		this.scheduledPayDate = payDayOf(payoutMonth.plusMonths(1), calendar);
		// 정산 내역은 배치가 만든다
		changeStatus(PayoutStatus.CALCULATED, PayoutHold.SYSTEM_MEMBER_ID, settledAt, null);
	}

	// 바깥에서 remove, clear로 정산 기록을 바꾸지 못하도록 읽기 전용으로 내보낸다 (추가는 addItem으로만)
	public List<PayoutItem> getItems() {
		return Collections.unmodifiableList(items);
	}

	public List<PayoutStatusHistory> getStatusHistories() {
		return Collections.unmodifiableList(statusHistories);
	}

	// 정산 항목(PayoutItem)은 정산 내역(Payout)을 통해서만 만든다 (payout_id NOT NULL)
	// 세미는 정산 대상일 전에 환불된 참가권이 빠지므로 판매(SALE)만 기록한다 (payout.md 2-3)
	public PayoutItem addItem(int orderItemId, long price, BigDecimal feeRate, LocalDate targetDate) {
		PayoutItem item = new PayoutItem(this, orderItemId, PayoutRecordType.SALE, price, feeRate, targetDate);
		items.add(item);

		// 수수료는 참가권 1장마다 원 미만을 버리고, 정산 내역의 수수료는 그 합이다 (payout.md 2-1)
		salesAmount += price;
		feeAmount += item.getFeeAmount();
		// 정책 공식(판매액 − 환불액 − 수수료)과 같은 모양으로 둔다. 세미는 refundAmount가 항상 0이다:
		// 대상일 전 환불은 항목이 안 생기고, 대상일 후 환불은 막는다(PRO-28). 최종에서는 대상일~지급 사이 환불이
		// 음수 정산 항목(PRO-32), 지급 뒤 환불은 회수(PRO-33)가 된다 (payout.md 2-2, 4-2)
		payoutAmount = salesAmount - refundAmount - feeAmount;
		return item;
	}

	// 저장해서 ID를 받은 뒤 부른다. 하루 65,536건을 넘지 않는 한 같은 날 같은 코드가 나오지 않는다
	public void assignPayoutCode() {
		this.payoutCode = "ST-%s-%04X".formatted(settledAt.format(CODE_DATE), getId() & 0xFFFF);
	}

	// 세미의 지급 완료는 실제 송금 없이 상태만 바꾸는 모의 지급이다 (payout.md 1장 지급 6번)
	// 보류된 정산 내역은 지급 완료로 바꿀 수 없다. 배치가 CALCULATED만 고르지만, 잘못 불려도 여기서 막는다
	public void pay(LocalDateTime paidAt) {
		if (status != PayoutStatus.CALCULATED) {
			throw new IllegalStateException("정산 예정 상태가 아니라 지급할 수 없습니다. payoutId=%d, status=%s"
				.formatted(getId(), status));
		}
		// 지급은 지급 배치가 한다
		changeStatus(PayoutStatus.PAID, PayoutHold.SYSTEM_MEMBER_ID, paidAt, null);
		this.paidAt = paidAt;
	}

	// 지급보류 걸기: 정산 예정(CALCULATED)일 때만 건다. 세미는 활성 보류를 하나만 둔다 (PRO-26)
	// 보류 기록(PayoutHold)은 여기서만 만든다. 저장은 호출한 쪽이 한다
	public PayoutHold hold(PayoutHoldReason reasonType, String reason, int heldBy, LocalDateTime heldAt) {
		if (status == PayoutStatus.HELD) {
			throw new PayoutHoldConflictException("이미 지급보류 중인 정산 내역입니다.");
		}
		if (status == PayoutStatus.PAID) {
			throw new PayoutHoldConflictException("이미 지급 완료된 정산 내역입니다.");
		}
		if (status == PayoutStatus.CANCELED) {
			throw new PayoutHoldConflictException("취소된 정산 내역입니다.");
		}
		changeStatus(PayoutStatus.HELD, heldBy, heldAt, reasonType.getKorean());
		return new PayoutHold(this, reasonType, reason, heldBy, heldAt);
	}

	// 지급보류 풀기: 정산 예정으로 돌리고 지급 예정일을 해제일 뒤 돌아오는 지급일로 다시 정한다 (payout.md 2-6)
	// 단, 원래 지급 예정일보다 앞당기지 않는다 (PRO-128). 보류를 걸 때는 지급 예정일을 바꾸지 않으므로
	// 지금 값이 원래 지급 예정일(또는 이전 해제로 이미 늦춰진 날)이다
	// 지급 배치는 상태와 지급 예정일만 보므로, 보류 기록을 보지 않아도 다음 지급일에 지급된다
	public void release(PayoutHold hold, int releasedBy, LocalDateTime releasedAt, BusinessDayCalendar calendar) {
		// ==로 비교하지 않는다. LAZY로 읽은 hold.getPayout()은 Hibernate 프록시라 같은 행이어도 this와 다른 객체다
		// 프록시의 getId()는 DB를 읽지 않고 들고 있는 ID를 돌려준다
		if (hold.getPayout().getId() != getId()) {
			throw new IllegalArgumentException("이 정산 내역의 지급보류가 아닙니다. payoutId=%d".formatted(getId()));
		}
		// 대회 취소로 닫힌 정산 내역에는 보류 기록이 HELD로 남아 있을 수 있다. 풀어도 지급 예정으로 돌리지 않는다
		if (status == PayoutStatus.CANCELED) {
			throw new PayoutHoldConflictException("취소된 정산 내역의 지급보류는 풀 수 없습니다.");
		}
		hold.markReleased(releasedBy, releasedAt);
		changeStatus(PayoutStatus.CALCULATED, releasedBy, releasedAt, null);
		LocalDate payDateAfterRelease = nextPayDateAfter(releasedAt.toLocalDate(), calendar);
		if (payDateAfterRelease.isAfter(scheduledPayDate)) {
			this.scheduledPayDate = payDateAfterRelease;
		}
	}

	// 대회 취소로 닫기: 지급 전(CALCULATED · HELD) 정산 내역을 CANCELED로 바꿔 다시는 지급되지 않게 한다 (PRO-34)
	// 금액 칸은 만들 때 그대로 둔다(얼마가 나갈 뻔했는지 남긴다). 합계 · 지급 조회는 상태로 고르므로 CANCELED는 빠진다
	// 보류 중이었으면 보류 기록은 HELD로 두고 상태만 최신으로 바꾼다. 무엇이었는지는 상태 이력에 남는다
	// 이미 지급한 정산 내역은 닫지 않는다. 지급 뒤 대회 취소는 회수(PRO-33)가 맡는다
	public void cancel(int canceledBy, LocalDateTime canceledAt, String reason) {
		if (status == PayoutStatus.PAID) {
			throw new IllegalStateException("이미 지급 완료된 정산 내역은 취소할 수 없습니다. payoutId=%d".formatted(getId()));
		}
		if (status == PayoutStatus.CANCELED) {
			throw new IllegalStateException("이미 취소된 정산 내역입니다. payoutId=%d".formatted(getId()));
		}
		changeStatus(PayoutStatus.CANCELED, canceledBy, canceledAt, reason);
	}

	private void changeStatus(PayoutStatus to, int changedBy, LocalDateTime changedAt, String reason) {
		statusHistories.add(new PayoutStatusHistory(this, status, to, changedBy, changedAt, reason));
		this.status = to;
	}

	// date "뒤"의 지급일: 이번 달 지급일이 date보다 늦으면 그날, 아니면(당일 포함) 다음 달 지급일
	// 20일이 주말이면 지급일이 밀리므로, 20일이 아니라 밀린 날과 비교한다 (12/20 일 해제 → 12/21 월)
	// 보류 해제와 판매자 요약(지급 예정 정산 내역이 없을 때의 다음 정산일)이 같은 규칙을 쓴다
	public static LocalDate nextPayDateAfter(LocalDate date, BusinessDayCalendar calendar) {
		YearMonth month = YearMonth.from(date);
		LocalDate thisMonthPayDay = payDayOf(month, calendar);
		if (thisMonthPayDay.isAfter(date)) {
			return thisMonthPayDay;
		}
		return payDayOf(month.plusMonths(1), calendar);
	}

	private static LocalDate payDayOf(YearMonth month, BusinessDayCalendar calendar) {
		return calendar.nextOrSame(month.atDay(PAY_DAY_OF_MONTH));
	}
}
