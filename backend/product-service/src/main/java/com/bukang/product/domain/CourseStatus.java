package com.bukang.product.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 종목 상태: 정원이 모두 판매되면 정원 마감이 된다
 */
@Getter
@RequiredArgsConstructor
public enum CourseStatus {
	OPEN("접수 중"),
	SOLD_OUT("정원 마감");

	// 화면과 메시지에 쓰는 이름
	private final String label;
}
