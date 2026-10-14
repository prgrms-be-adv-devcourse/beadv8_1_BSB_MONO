package com.bukang.product.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 대회 판매 상태: 판매 예정 → 판매 중 → 판매 종료
 * 등록하면 판매 예정이고, 상태 배치가 판매 시작일 0시에 판매 중, 대회일 다음 날 0시에 판매 종료로 바꾼다.
 * 상태는 판매자 화면용이다. 구매자가 예매할 수 있는지는 크루 예매 시작·개인 예매 오픈 시각으로 따로 본다.
 * 판매 중지는 판매자가 잠시 멈춘 상태라 다시 열 수 있고, 판매 종료는 대회일이 지나 완전히 끝난 상태다.
 * 대회 취소는 판매자가 판매 종료 전에 대회를 취소한 상태로, 되돌릴 수 없다.
 */
@Getter
@RequiredArgsConstructor
public enum RaceSaleStatus {
	SCHEDULED("판매 예정"),
	ON_SALE("판매 중"),
	STOPPED("판매 중지"),
	CLOSED("판매 종료"),
	CANCELED("대회 취소");

	// 화면과 메시지에 쓰는 이름
	private final String label;
}
