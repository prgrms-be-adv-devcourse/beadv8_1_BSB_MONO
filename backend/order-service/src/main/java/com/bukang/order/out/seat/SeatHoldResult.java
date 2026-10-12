package com.bukang.order.out.seat;

import java.time.LocalDateTime;

/**
 * 상품 서비스(B)의 자리 확보 결과
 *
 * @param held      자리를 확보했는지. false면 남은 자리가 없다(매진)
 * @param expiresAt 결제 기한 (B의 RACE_SEAT.expires_at). 매진이면 null
 */
public record SeatHoldResult(boolean held, LocalDateTime expiresAt) {
	public static SeatHoldResult held(LocalDateTime expiresAt) {
		return new SeatHoldResult(true, expiresAt);
	}

	public static SeatHoldResult soldOut() {
		return new SeatHoldResult(false, null);
	}
}
