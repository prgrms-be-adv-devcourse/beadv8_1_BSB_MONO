package com.bukang.product.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 임시 확보한 자리의 상태: 결제 대기 → 판매 → (결제 취소) 환불 복구, 또는 결제 대기 → 해제
 */
@Getter
@RequiredArgsConstructor
public enum HoldStatus {
	HELD("결제 대기"),
	SOLD("판매"),
	// 결제하지 않고 끝난 자리 (활성 시간이 끝남)
	RELEASED("해제"),
	// 판매한 뒤 결제를 취소해 정원으로 되돌린 자리
	RESTORED("환불 복구");

	// 화면과 메시지에 쓰는 이름
	private final String label;
}
