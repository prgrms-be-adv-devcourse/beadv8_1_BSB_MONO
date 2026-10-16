package com.bukang.payout.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 회수 상태. 입금 기한이 지난 연체는 PRO-198에서 더할 수 있다
 */
@Getter
@RequiredArgsConstructor
public enum PayoutRecoveryStatus {
	REQUESTED("회수 요청"),
	COMPLETED("회수 완료");

	private final String korean;
}
