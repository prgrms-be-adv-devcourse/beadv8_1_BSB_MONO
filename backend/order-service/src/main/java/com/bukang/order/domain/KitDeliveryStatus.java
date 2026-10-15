package com.bukang.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 키트 배송 상태 (정책 5장 3 키트, PRO-136). 키트가 없는 주문 상품은 상태가 없다(null)
// 세미는 배송 준비 → 발송까지만 둔다. 배송 완료·송장 번호는 2차
@Getter
@RequiredArgsConstructor
public enum KitDeliveryStatus {
	PREPARING("배송 준비"),
	SHIPPED("발송");

	private final String label;
}
