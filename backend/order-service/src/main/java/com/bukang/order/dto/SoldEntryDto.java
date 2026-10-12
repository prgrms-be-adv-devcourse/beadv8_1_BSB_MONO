package com.bukang.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 정산할 참가권 1장 (결제되고 취소되지 않은 주문 상품). 정산 서비스의 SoldEntry와 필드 이름·뜻이 같다
 * 참가권은 수량 1이라 price는 참가권 1장 값(실제 결제 금액, 키트 포함)이다
 */
@AllArgsConstructor
@Getter
public class SoldEntryDto {
	private final int orderItemId;
	private final int sellerId;
	private final int raceId;
	private final long price;
}
