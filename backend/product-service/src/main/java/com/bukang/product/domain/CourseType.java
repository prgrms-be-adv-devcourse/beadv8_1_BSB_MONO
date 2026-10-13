package com.bukang.product.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 종목 구분 (검색 필터)
 * 5K·10K는 숫자로 시작해 상수 이름으로 쓸 수 없으므로 KM_5·KM_10으로 짓고, 화면에는 label을 보여 준다.
 */
@Getter
@RequiredArgsConstructor
public enum CourseType {
	KM_5("5K"),
	KM_10("10K"),
	HALF("하프"),
	FULL("풀"),
	ULTRA("울트라");

	// 화면과 메시지에 쓰는 이름
	private final String label;
}
