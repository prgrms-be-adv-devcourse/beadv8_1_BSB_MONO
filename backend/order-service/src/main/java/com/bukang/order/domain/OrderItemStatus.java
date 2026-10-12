package com.bukang.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 주문 상품 상태 (09 ERD order_item.status). 취소는 주문 상품 단위로 한다 (PRO-46)
@Getter
@RequiredArgsConstructor
public enum OrderItemStatus {
	ORDERED("결제 대기"),
	PAID("결제 완료"),
	CANCELED("취소");

	private final String label;
}
