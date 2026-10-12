package com.bukang.order.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 주문 목록 한 페이지. Spring Data의 Page를 그대로 내보내면 응답 모양이 버전마다 바뀔 수 있어 필요한 값만 옮긴다
@AllArgsConstructor
@Getter
public class OrderPageDto {
	private final List<OrderSummaryDto> orders;
	// 0부터 시작
	private final int page;
	private final int size;
	private final long totalElements;
	private final int totalPages;

	public static OrderPageDto from(Page<OrderSummaryDto> page) {
		return new OrderPageDto(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
			page.getTotalPages());
	}
}
