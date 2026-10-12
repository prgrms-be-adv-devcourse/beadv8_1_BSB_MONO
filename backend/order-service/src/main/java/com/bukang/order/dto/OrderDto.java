package com.bukang.order.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.bukang.order.domain.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class OrderDto {
	private final int id;
	// 주문 일시
	private final LocalDateTime createDate;
	private final String orderNo;
	private final int buyerId;
	private final int raceId;
	private final OrderStatus status;
	private final int totalAmount;
	// 취소된 주문 상품 금액의 합 (PRO-46)
	private final int canceledAmount;
	// 결제 기한 (화면용 사본)
	private final LocalDateTime paymentDueAt;
	// 결제 완료 일시. 결제 전이면 null
	private final LocalDateTime paidAt;
	private final List<OrderItemDto> items;
}
