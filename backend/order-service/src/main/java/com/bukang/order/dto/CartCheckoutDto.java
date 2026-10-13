package com.bukang.order.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CartCheckoutDto {
	// 주문을 만들었거나 이미 만든 건수 (CREATED + EXISTING)
	private final int orderedCount;
	// 요청한 순서대로
	private final List<CartCheckoutItemDto> results;
}
