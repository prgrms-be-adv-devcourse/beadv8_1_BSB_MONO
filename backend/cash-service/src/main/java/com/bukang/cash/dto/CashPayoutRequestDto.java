package com.bukang.cash.dto;

import com.bukang.cash.domain.payout.PayoutStage;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "정산 지급 요청")
@AllArgsConstructor
@Getter
public class CashPayoutRequestDto {
	@Schema(description = "정산액을 받을 판매자 회원 ID (지갑 주인)", example = "10")
	@NotNull(message = "판매자 회원 ID를 작성해주세요.")
	@Positive(message = "판매자 회원 ID는 0보다 커야 합니다.")
	private final Integer sellerId;

	@Schema(description = "정산 단계 (FIRST: 1차, FINAL: 최종)", example = "FINAL")
	@NotNull(message = "정산 단계를 작성해주세요.")
	private final PayoutStage payoutStage;

	// 최종 정산은 회수 상계로 0원이 될 수 있다 (이때도 수수료는 남아있을 수 있어 요청은 들어온다)
	@Schema(description = "판매자에게 지급할 정산 대금 (최종 정산은 회수 상계로 0원이 될 수 있음)", example = "90000")
	@NotNull(message = "정산 대금을 작성해주세요.")
	@PositiveOrZero(message = "정산 대금은 0 이상이어야 합니다.")
	private final Integer settlementAmount;

	@Schema(description = "시스템 지갑으로 보낼 수수료 (1차 정산은 0)", example = "5500")
	@NotNull(message = "수수료를 작성해주세요.")
	@PositiveOrZero(message = "수수료는 0 이상이어야 합니다.")
	private final Integer feeAmount;
}
