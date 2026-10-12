package com.bukang.order.domain;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import com.bukang.common.global.jpa.entity.BaseIdAndTime;
import com.bukang.order.domain.exception.OrderNotExtendableException;
import com.bukang.order.domain.exception.OrderNotPayableException;
import com.bukang.order.dto.OrderDto;
import com.bukang.order.dto.OrderSummaryDto;
import com.bukang.order.out.race.RaceCourseInfo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문. 개인 예매는 결제 1건 = 참가 신청 1건이라 주문 상품이 1개다 (ERD 추천안 v4 연결점, 09 ERD orders)
 * 금액은 주문 당시 값을 남긴다. 구매자·대회는 다른 서비스 소유라 FK 없이 ID 값만 둔다
 */
@Entity
@NoArgsConstructor(access = PROTECTED)
@Getter
@Table(
	name = "ORDER_ORDER",
	uniqueConstraints = {
		// 같은 요청을 두 번 보내도 주문은 하나 (정책 4장 2-3)
		@UniqueConstraint(name = "uk_order_idempotency", columnNames = {"buyer_id", "idempotency_key"})
	},
	indexes = {
		// 내 주문 목록 최신순 (PRO-43)
		@Index(name = "idx_order_buyer", columnList = "buyer_id, id"),
		// 결제 기한이 지난 결제 대기 주문 찾기 (만료 정리 배치, PRO-42)
		@Index(name = "idx_order_due", columnList = "status, payment_due_at")
	}
)
public class Order extends BaseIdAndTime {
	private static final DateTimeFormatter NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

	// 주문 번호: OD-주문한 날-16진수 4자리 (예: OD-20261012-3F9A). 정산 ID(ST-)와 같은 모양, ID를 받은 뒤 정한다
	@Column(name = "order_no", length = 30)
	private String orderNo;

	@Column(name = "buyer_id", nullable = false)
	private int buyerId;

	@Column(name = "race_id", nullable = false)
	private int raceId;

	@Column(name = "total_amount", nullable = false)
	private int totalAmount;

	@Column(name = "canceled_amount", nullable = false)
	private int canceledAmount;

	@Enumerated(STRING)
	@Column(name = "status", nullable = false, columnDefinition = "varchar(20)")
	private OrderStatus status;

	@Column(name = "idempotency_key", nullable = false, length = 64)
	private String idempotencyKey;

	// 화면·정리용 사본. 결제와 만료 판정의 원본은 상품 서비스의 RACE_SEAT.expires_at이다
	@Column(name = "payment_due_at", nullable = false)
	private LocalDateTime paymentDueAt;

	@Column(name = "paid_at")
	private LocalDateTime paidAt;

	// 결제 기한을 늘린 시각. 늘리기는 주문당 한 번이라 값이 있으면 다시 늘리지 않는다 (정책 4장 결제 2분 연장)
	@Column(name = "deadline_extended_at")
	private LocalDateTime deadlineExtendedAt;

	// 주문 목록(PRO-43)에서 주문마다 상품을 따로 읽지 않도록 여러 주문의 상품을 한 번에 읽는다
	@BatchSize(size = 100)
	@OneToMany(mappedBy = "order", cascade = ALL)
	private List<OrderItem> items = new ArrayList<>();

	private Order(int buyerId, int raceId, String idempotencyKey, LocalDateTime paymentDueAt) {
		this.buyerId = buyerId;
		this.raceId = raceId;
		this.idempotencyKey = idempotencyKey;
		this.paymentDueAt = paymentDueAt;
		this.status = OrderStatus.CREATED;
	}

	// 바로 결제 예매: 자리를 확보한 참가 신청 1건으로 주문 상품 1개짜리 주문을 만든다
	public static Order forParticipation(Participation participation, RaceCourseInfo course, String idempotencyKey,
		LocalDateTime paymentDueAt) {
		Order order = new Order(participation.getMemberId(), course.raceId(), idempotencyKey, paymentDueAt);
		order.items.add(OrderItem.ticket(order, participation, course));
		order.totalAmount = order.items.stream().mapToInt(OrderItem::getAmount).sum();
		return order;
	}

	// 저장해 ID를 받은 뒤 부른다
	public void assignOrderNo() {
		this.orderNo = "OD-%s-%04X".formatted(getCreateDate().format(NO_DATE), getId() & 0xFFFF);
	}

	// 이 주문이 해당 참가 신청으로 만든 주문인지 (같은 Idempotency-Key를 다른 신청서에 다시 쓴 요청 가려내기)
	public boolean isFor(int participationId) {
		return items.stream().anyMatch(item -> item.getParticipationId() == participationId);
	}

	// 결제할 수 있는지: 결제 대기 중이고 결제 기한(사본) 전이어야 한다. 최종 판정은 상품 서비스의 자리 판매가 한다
	public void checkPayable(LocalDateTime now) {
		if (status != OrderStatus.CREATED) {
			throw new OrderNotPayableException(
				"결제할 수 없는 주문입니다(현재 상태: %s).".formatted(status.getLabel()));
		}
		if (!now.isBefore(paymentDueAt)) {
			throw new OrderNotPayableException("결제 기한이 지났습니다.");
		}
	}

	public boolean isPaid() {
		return status == OrderStatus.PAID;
	}

	// 예치금 차감과 자리 판매가 끝났다. 주문과 주문 상품을 함께 결제 완료로 바꾼다
	public void markPaid(LocalDateTime paidAt) {
		if (status != OrderStatus.CREATED) {
			throw new OrderNotPayableException(
				"결제할 수 없는 주문입니다(현재 상태: %s).".formatted(status.getLabel()));
		}
		this.status = OrderStatus.PAID;
		this.paidAt = paidAt;
		items.forEach(OrderItem::markPaid);
	}

	// 개인 예매 주문은 참가 신청 1건짜리다 (결제 1건 = 참가 신청 1건)
	public int participationId() {
		return items.getFirst().getParticipationId();
	}

	// 기한을 늘릴 수 있는지: 결제 대기 중이고, 아직 늘리지 않았고, 기한 전이어야 한다
	public void checkExtendable(LocalDateTime now) {
		if (status != OrderStatus.CREATED) {
			throw new OrderNotExtendableException(
				"결제 대기 중인 주문만 결제 기한을 늘릴 수 있습니다(현재 상태: %s).".formatted(status.getLabel()));
		}
		if (deadlineExtendedAt != null) {
			throw new OrderNotExtendableException("결제 기한은 한 번만 늘릴 수 있습니다.");
		}
		if (!now.isBefore(paymentDueAt)) {
			throw new OrderNotExtendableException("결제 기한이 지났습니다.");
		}
	}

	public void extendDeadline(LocalDateTime newPaymentDueAt, LocalDateTime now) {
		checkExtendable(now);
		this.paymentDueAt = newPaymentDueAt;
		this.deadlineExtendedAt = now;
	}

	public boolean isOwnedBy(int memberId) {
		return buyerId == memberId;
	}

	public OrderDto toDto() {
		return new OrderDto(
			getId(),
			getCreateDate(),
			orderNo,
			buyerId,
			raceId,
			status,
			totalAmount,
			canceledAmount,
			paymentDueAt,
			paidAt,
			items.stream().map(OrderItem::toDto).toList()
		);
	}

	public OrderSummaryDto toSummaryDto() {
		return new OrderSummaryDto(getId(), getCreateDate(), orderNo, status, totalAmount, paymentDueAt,
			items.getFirst().getItemName(), items.size());
	}
}
