package com.bukang.product.dto;

/**
 * 자리 판매(결제 대기 → 판매) 결과 (주문 서비스 SeatSellResult와 같은 값)
 */
public enum HoldSellResult {
	SOLD,
	// 끝 시각이 지났거나 이미 해제된 자리라 판매하지 않았다 (주문 서비스는 결제를 취소하고 예치금을 돌려준다)
	EXPIRED
}
