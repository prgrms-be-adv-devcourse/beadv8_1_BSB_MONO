package com.bukang.order.domain;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

import com.bukang.common.global.jpa.entity.BaseIdAndTime;
import com.bukang.order.domain.exception.KitNotShippableException;
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
 * 정산(E)은 이 행의 판매자·대회·대회 날짜·금액을 읽는다 (정산 SoldEntryPort → /internal/v1/order/sold-entries)
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
		// 같음 조건(status = PAID)을 앞에, 범위 조건(race_date)을 뒤에 둬야 두 조건을 모두 인덱스로 좁힌다
		// 이름을 바꾼 건 dev(ddl-auto: update)가 같은 이름의 옛 인덱스를 고치지 않아서다
		@Index(name = "idx_order_item_status_race_date", columnList = "status, race_date")
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

	// 키트 배송 상태. 키트가 있는 종목만 배송 준비로 시작하고, 없으면 null (PRO-136)
	@Enumerated(STRING)
	@Column(name = "kit_delivery_status", columnDefinition = "varchar(20)")
	private KitDeliveryStatus kitDeliveryStatus;

	@Column(name = "kit_shipped_at")
	private LocalDateTime kitShippedAt;

	static OrderItem ticket(Order order, Participation participation, RaceCourseInfo course) {
		OrderItem item = new OrderItem();
		item.order = order;
		item.participationId = participation.getId();
		item.raceId = course.raceId();
		item.raceCourseId = course.raceCourseId();
		item.sellerId = course.sellerId();
		item.raceDate = course.raceDate();
		item.itemName = course.courseName();
		item.unitPrice = course.price();
		item.quantity = TICKET_QUANTITY;
		item.amount = item.unitPrice * TICKET_QUANTITY;
		item.status = OrderItemStatus.ORDERED;
		item.kitDeliveryStatus = course.kitIncluded() ? KitDeliveryStatus.PREPARING : null;
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

	// 판매자가 키트를 보냈다. 결제한 키트만, 한 번만 발송한다
	// 환불 기한이 지났는지는 종목 정보를 알아야 해서 부르는 쪽(KitDeliveryUseCase)이 확인한다
	public void shipKit(LocalDateTime shippedAt) {
		checkKitShippable();
		this.kitDeliveryStatus = KitDeliveryStatus.SHIPPED;
		this.kitShippedAt = shippedAt;
	}

	// 키트를 보낼 수 있는 주문 상품인지 (키트 있음, 아직 안 보냄, 결제 완료)
	public void checkKitShippable() {
		if (kitDeliveryStatus == null) {
			throw new KitNotShippableException("키트가 없는 참가권입니다.");
		}
		if (kitDeliveryStatus == KitDeliveryStatus.SHIPPED) {
			throw new KitNotShippableException("이미 발송한 키트입니다.");
		}
		if (status != OrderItemStatus.PAID) {
			throw new KitNotShippableException(
				"결제한 참가권의 키트만 발송할 수 있습니다(현재 상태: %s).".formatted(status.getLabel()));
		}
	}

	public boolean isSoldBy(int sellerId) {
		return this.sellerId == sellerId;
	}

	public OrderItemDto toDto() {
		return new OrderItemDto(getId(), participationId, raceCourseId, sellerId, itemName, unitPrice, quantity,
			amount, status, kitDeliveryStatus, kitShippedAt);
	}
}
