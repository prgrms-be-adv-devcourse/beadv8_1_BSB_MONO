package com.bukang.order.out.seat;

// 상품 서비스(B)의 자리 판매(HELD → SOLD) 결과. 기한(expires_at)이 지났으면 판매되지 않는다
public enum SeatSellResult {
	SOLD,
	EXPIRED
}
