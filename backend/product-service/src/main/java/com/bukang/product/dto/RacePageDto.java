package com.bukang.product.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 공개 대회 목록 한 페이지 (주문 서비스 OrderPageDto와 같은 모양)
@AllArgsConstructor
@Getter
public class RacePageDto {
	private final List<RaceSummaryDto> races;
	// 0부터 시작
	private final int page;
	private final int size;
	// 조건에 맞는 대회 수 (종목 수가 아니다)
	private final long totalElements;
	private final int totalPages;

	public static RacePageDto of(List<RaceSummaryDto> races, int page, int size, long totalElements) {
		int totalPages = (int)((totalElements + size - 1) / size);
		return new RacePageDto(races, page, size, totalElements, totalPages);
	}
}
