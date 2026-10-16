package com.bukang.order.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 판매자 키트 주문 목록 한 페이지 (PRO-197). Page를 그대로 내보내지 않고 필요한 값만 옮긴다 (OrderPageDto와 같은 모양)
@AllArgsConstructor
@Getter
public class SellerKitPageDto {
	private final List<SellerKitDto> kits;
	// 0부터 시작
	private final int page;
	private final int size;
	private final long totalElements;
	private final int totalPages;

	public static SellerKitPageDto from(Page<SellerKitDto> page) {
		return new SellerKitPageDto(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
			page.getTotalPages());
	}
}
