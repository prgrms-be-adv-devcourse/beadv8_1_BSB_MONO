package com.bukang.product.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 대회 판매 상태: 작성 중 → 판매 예정 → 판매 중 → 판매 종료
 * 판매 중지는 판매자가 잠시 멈춘 상태라 다시 열 수 있고, 판매 종료는 대회일이 지나 완전히 끝난 상태다.
 * 대회 취소는 판매자가 판매 종료 전에 대회를 취소한 상태로, 되돌릴 수 없다.
 */
@Getter
@RequiredArgsConstructor
public enum RaceSaleStatus {
	DRAFT("작성 중"),
	SCHEDULED("판매 예정"),
	ON_SALE("판매 중"),
	STOPPED("판매 중지"),
	CLOSED("판매 종료"),
	CANCELED("대회 취소");

	// 화면과 메시지에 쓰는 이름
	private final String label;
}
