package com.bukang.payout.domain;

import com.bukang.payout.domain.exception.PayoutQueryException;

/**
 * 건별 정산 조회의 페이지 규칙
 * 건별 정산은 참가권 1장이 한 줄이라, 1개월 한도 안에서도 큰 대회 하나면 수천 줄이 된다 (샘플: 9월 2,700줄)
 * 정산 내역(판매자 × 대회 × 월)과 지급보류는 기간 한도만으로 줄 수가 작아 페이지를 나누지 않는다
 */
public final class PayoutItemPaging {
	public static final int DEFAULT_SIZE = 50;
	public static final int MAX_SIZE = 100;

	private PayoutItemPaging() {
	}

	// 페이지 번호는 Spring Data처럼 0부터 센다
	public static void check(int page, int size) {
		if (page < 0) {
			throw new PayoutQueryException("페이지 번호는 0 이상이어야 합니다.");
		}
		if (size < 1 || size > MAX_SIZE) {
			throw new PayoutQueryException("페이지 크기는 1 이상 100 이하여야 합니다.");
		}
	}
}
