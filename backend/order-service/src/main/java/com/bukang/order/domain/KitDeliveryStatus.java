package com.bukang.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 키트 배송 상태 (정책 5장 3 키트, 시안 S08, PRO-136). 키트가 없는 주문 상품은 상태가 없다(null)
// 배송 준비 → 배송 중(택배사·송장 번호를 넣으면) → 배송 완료 순서로만 바뀐다
@Getter
@RequiredArgsConstructor
public enum KitDeliveryStatus {
	PREPARING("배송 준비"),
	SHIPPING("배송 중"),
	DELIVERED("배송 완료");

	private final String label;
}
