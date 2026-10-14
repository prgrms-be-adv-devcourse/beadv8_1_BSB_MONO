package com.bukang.product.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 자리 확보 결과 (주문 서비스 SeatHoldResult와 같은 모양). 매진도 오류가 아니라 결과로 돌려준다
@AllArgsConstructor
@Getter
public class HoldResultDto {
	// 자리를 확보했는지. false면 남은 자리가 없다(매진)
	private final boolean held;
	// 임시 확보 끝 시각 (요청한 값 그대로). 매진이면 null
	private final LocalDateTime expiresAt;

	public static HoldResultDto held(LocalDateTime expiresAt) {
		return new HoldResultDto(true, expiresAt);
	}

	public static HoldResultDto soldOut() {
		return new HoldResultDto(false, null);
	}
}
