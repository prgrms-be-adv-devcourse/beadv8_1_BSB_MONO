package com.bukang.product.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 종목 노면 (검색 필터)
 */
@Getter
@RequiredArgsConstructor
public enum CourseSurface {
	ROAD("로드"),
	TRAIL("트레일");

	// 화면과 메시지에 쓰는 이름
	private final String label;
}
