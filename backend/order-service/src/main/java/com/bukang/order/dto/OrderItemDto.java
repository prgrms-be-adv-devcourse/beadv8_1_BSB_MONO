package com.bukang.order.dto;

import java.time.LocalDateTime;

import com.bukang.order.domain.KitDeliveryStatus;
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
	// 키트 배송 상태 (PREPARING 배송 준비 / SHIPPED 발송). 키트가 없는 참가권은 null
	private final KitDeliveryStatus kitDeliveryStatus;
	// 키트를 발송한 시각. 발송 전이면 null
	private final LocalDateTime kitShippedAt;
}
