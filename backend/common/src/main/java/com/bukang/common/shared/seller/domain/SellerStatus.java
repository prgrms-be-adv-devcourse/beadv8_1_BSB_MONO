package com.bukang.common.shared.seller.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 판매자 승인 상태: 등록 신청 → 관리자가 승인 또는 반려한다
 */
@Getter
@RequiredArgsConstructor
public enum SellerStatus {
	PENDING("승인 대기"),
	APPROVED("승인"),
	REJECTED("반려");

	// 화면과 메시지에 쓰는 이름
	private final String label;
}
