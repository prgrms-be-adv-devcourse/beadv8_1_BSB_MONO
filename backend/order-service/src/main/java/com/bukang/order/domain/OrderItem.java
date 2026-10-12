package com.bukang.order.domain;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

import com.bukang.common.global.jpa.entity.BaseIdAndTime;
import com.bukang.order.domain.exception.OrderItemNotCancelableException;
import com.bukang.order.dto.OrderItemDto;
import com.bukang.order.out.race.RaceCourseInfo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 상품. 참가권 1장(키트 포함) = 1행이고 수량은 늘 1이다 (09 ERD order_item, 굿즈 삭제)
 * 정산(E)은 이 행의 판매자·대회·금액을 읽는다 (PayoutSourcePort)
 */
@Entity
@NoArgsConstructor(access = PROTECTED)
@Getter
@Table(
	name = "ORDER_ORDER_ITEM",
	uniqueConstraints = {
		// 결제 1건 = 참가 신청 1건 (ERD 추천안 v4 연결점). 같은 신청서로 주문이 두 번 생기지 않는다
		@UniqueConstraint(name = "uk_order_item_participation", columnNames = {"participation_id"})
	},
	indexes = {
		// 정산 배치가 대회 날짜로 결제된 참가권을 찾는다 (정산용 판매 내역 조회)
		@Index(name = "idx_order_item_race_date", columnList = "race_date, status")
	}
)
public class OrderItem extends BaseIdAndTime {
	private static final int TICKET_QUANTITY = 1;

	@ManyToOne(fetch = LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@Column(name = "participation_id", nullable = false)
	private int participationId;

	@Column(name = "race_id", nullable = false)
	private int raceId;

	@Column(name = "race_course_id", nullable = false)
	private int raceCourseId;

	@Column(name = "seller_id", nullable = false)
	private int sellerId;

	// 대회 일시 사본 (주문 당시 종목 정보). 정산 배치가 대회 날짜로 판매 내역을 찾는 조건이다
	@Column(name = "race_date", nullable = false)
	private LocalDateTime raceDate;

	// 주문 당시 상품명·단가. 판매자가 나중에 고쳐도 주문 내역은 바뀌지 않는다
	@Column(name = "item_name", nullable = false, length = 200)
	private String itemName;

	@Column(name = "unit_price", nullable = false)
	private int unitPrice;

	@Column(name = "quantity", nullable = false)
	private int quantity;

	// 정산 판매액 = 실제 결제 금액 (정책 6장 1-5)
	@Column(name = "amount", nullable = false)
	private int amount;

	@Enumerated(STRING)
	@Column(name = "status", nullable = false, columnDefinition = "varchar(20)")
	private OrderItemStatus status;

	@Column(name = "canceled_amount", nullable = false)
	private int canceledAmount;

	@Column(name = "canceled_at")
	private LocalDateTime canceledAt;

	static OrderItem ticket(Order order, Participation participation, RaceCourseInfo course) {
		OrderItem item = new OrderItem();
		item.order = order;
		item.participationId = participation.getId();
		item.raceId = course.raceId();
		item.raceCourseId = course.raceCourseId();
		item.sellerId = course.sellerId();
		item.raceDate = course.raceDate();
		item.itemName = course.courseName();
		item.unitPrice = course.ticketPrice();
		item.quantity = TICKET_QUANTITY;
		item.amount = item.unitPrice * TICKET_QUANTITY;
		item.status = OrderItemStatus.ORDERED;
		return item;
	}

	void markPaid() {
		this.status = OrderItemStatus.PAID;
	}

	// 결제한 상품만 취소할 수 있다. 같은 취소가 두 번 오면 두 번째는 여기서 막혀 환불도 한 번만 일어난다
	public void checkCancelable() {
		if (status == OrderItemStatus.CANCELED) {
			throw new OrderItemNotCancelableException("이미 취소한 상품입니다.");
		}
		if (status != OrderItemStatus.PAID) {
			throw new OrderItemNotCancelableException(
				"결제한 상품만 취소할 수 있습니다(현재 상태: %s).".formatted(status.getLabel()));
		}
	}

	void cancel(LocalDateTime canceledAt) {
		checkCancelable();
		this.status = OrderItemStatus.CANCELED;
		this.canceledAmount = amount;
		this.canceledAt = canceledAt;
	}

	public OrderItemDto toDto() {
		return new OrderItemDto(getId(), participationId, raceCourseId, sellerId, itemName, unitPrice, quantity,
			amount, status);
	}
}
