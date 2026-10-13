package com.bukang.common.shared.seller.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 판매자 구분 (정책 1장 2-2)
 */
@Getter
@RequiredArgsConstructor
public enum SellerType {
	BUSINESS("사업자"),
	INDIVIDUAL("비사업자");

	// 화면과 메시지에 쓰는 이름
	private final String label;
}
