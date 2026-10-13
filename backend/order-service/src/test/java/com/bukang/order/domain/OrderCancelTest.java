package com.bukang.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bukang.order.domain.exception.OrderItemNotCancelableException;
import com.bukang.order.out.race.RaceCourseInfo;

/**
 * 주문 상품 단위 취소에 따른 주문 상태 (PARTIAL_CANCELED / CANCELED)
 * 지금 주문은 상품 1개짜리뿐이라(장바구니 결제 전), 상품 2개짜리 주문을 직접 만들어 확인한다
 */
class OrderCancelTest {
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 12, 10, 0);
	private static final RaceCourseInfo COURSE = new RaceCourseInfo(10, 100, NOW.plusDays(60), NOW.minusDays(1),
		NOW.plusDays(30), 14, null, 7, "종목", 50_000, 10_000, true,
		LocalDate.of(2026, 11, 20), null);

	@Test
	@DisplayName("상품 2개 중 1개를 취소하면 PARTIAL_CANCELED, 나머지도 취소하면 CANCELED가 되고 취소 금액이 쌓인다")
	void cancelItems() {
		Order order = paidOrderWithTwoItems();
		OrderItem first = order.getItems().get(0);
		OrderItem second = order.getItems().get(1);

		order.cancelItem(first, NOW);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PARTIAL_CANCELED);
		assertThat(order.getCanceledAmount()).isEqualTo(60_000);
		assertThat(first.getStatus()).isEqualTo(OrderItemStatus.CANCELED);
		assertThat(first.getCanceledAmount()).isEqualTo(60_000);
		assertThat(first.getCanceledAt()).isEqualTo(NOW);
		assertThat(second.getStatus()).isEqualTo(OrderItemStatus.PAID);

		order.cancelItem(second, NOW.plusMinutes(1));
		assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
		assertThat(order.getCanceledAmount()).isEqualTo(120_000);
	}

	@Test
	@DisplayName("이미 취소한 상품을 다시 취소하면 거절되고 취소 금액은 그대로다")
	void cancelTwice() {
		Order order = paidOrderWithTwoItems();
		OrderItem first = order.getItems().get(0);
		order.cancelItem(first, NOW);

		assertThatThrownBy(() -> order.cancelItem(first, NOW))
			.isInstanceOf(OrderItemNotCancelableException.class)
			.hasMessage("이미 취소한 상품입니다.");
		assertThat(order.getCanceledAmount()).isEqualTo(60_000);
	}

	private static Order paidOrderWithTwoItems() {
		Order order = Order.forParticipation(participation(1), COURSE, "key", NOW.plusMinutes(10));
		order.getItems().add(OrderItem.ticket(order, participation(2), COURSE));
		order.markPaid(NOW);
		return order;
	}

	private static Participation participation(int memberId) {
		ParticipantInfo info = new ParticipantInfo("김러너", LocalDate.of(1994, 6, 25), Gender.MALE, "010-1234-5678",
			"김가족", "010-9876-5432", "L", "서울 성동구 왕십리로 00");
		return Participation.individual(memberId, 100, 10, info, NOW);
	}
}
