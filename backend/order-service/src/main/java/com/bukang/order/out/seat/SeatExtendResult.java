package com.bukang.order.out.seat;

import java.time.LocalDateTime;

/**
 * 상품 서비스(B)의 결제 기한 연장 결과
 *
 * @param outcome   연장됨 / 이미 한 번 늘림 / 기한이 지나 자리가 없음
 * @param expiresAt 늘어난 결제 기한 (B의 RACE_SEAT.expires_at). 연장되지 않았으면 null
 */
public record SeatExtendResult(Outcome outcome, LocalDateTime expiresAt) {
	public enum Outcome {
		EXTENDED,
		ALREADY_EXTENDED,
		EXPIRED
	}

	public static SeatExtendResult extended(LocalDateTime expiresAt) {
		return new SeatExtendResult(Outcome.EXTENDED, expiresAt);
	}

	public static SeatExtendResult alreadyExtended() {
		return new SeatExtendResult(Outcome.ALREADY_EXTENDED, null);
	}

	public static SeatExtendResult expired() {
		return new SeatExtendResult(Outcome.EXPIRED, null);
	}
}
