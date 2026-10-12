package com.bukang.order.dto;

import com.bukang.order.domain.OrderItemStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class OrderItemDto {
	private final int id;
	private final int participationId;
	private final int raceCourseId;
	private final int sellerId;
	// 주문 당시 상품명·단가
	private final String itemName;
	private final int unitPrice;
	private final int quantity;
	private final int amount;
	private final OrderItemStatus status;
}
