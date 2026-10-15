package com.bukang.payout.global.in;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bukang.common.global.rsdata.RsData;
import com.bukang.payout.app.RefundEligibilityUseCase;
import com.bukang.payout.dto.RefundEligibilityDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 서비스끼리 부르는 내부 API. 게이트웨이에 라우팅하지 않는다(/internal/** 경로)
 * 환불 가능 여부는 정산 규칙(정산 대상일)으로 정해지므로, 주문 서비스가 규칙을 복사하지 않고 여기에 물어본다
 */
@Tag(name = "PayoutInternal", description = "참가권 환불 가능 여부 조회 API (서비스 간 내부 호출 전용)")
@RestController
@RequestMapping("/internal/v1/payout")
@RequiredArgsConstructor
public class InternalV1PayoutRefundController {
	// 서버 시간대와 상관없이 한국 날짜로 판단한다 (정산 대상일 배치와 같은 기준)
	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	private final RefundEligibilityUseCase refundEligibilityUseCase;

	@GetMapping("/refund-eligibility")
	@Operation(summary = "참가권 환불 가능 여부 조회",
		description = "대회 ID로 환불 마감일을 찾아, 오늘(한국 날짜)이 마감일 이전(마감일 당일 포함)이면 환불 가능으로 돌려준다. "
			+ "마감일 다음 날(1차 정산 대상일)부터는 사유와 상관없이(대회 취소·일정 변경 포함) 환불할 수 없다. "
			+ "환불 마감일을 찾지 못하거나 대회 ID가 없으면 대회일 기준(대회 다음 날부터 불가)으로 판단한다. "
			+ "주문 서비스가 환불 요청을 받을 때 이 API를 동기 호출로 부른다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공 (환불 가능 여부는 data.refundable)",
			content = @Content(examples = {
				@ExampleObject(name = "환불 가능", value = PayoutInternalApiExamples.REFUNDABLE),
				@ExampleObject(name = "환불 불가 (환불 마감일 지남)", value = PayoutInternalApiExamples.NOT_REFUNDABLE)
			})),
		@ApiResponse(responseCode = "400", description = "대회일 없음 또는 대회일 형식 오류",
			content = @Content(schema = @Schema(implementation = RsData.class), examples = {
				@ExampleObject(name = "대회일 없음", value = PayoutInternalApiExamples.MISSING_PARAMETER),
				@ExampleObject(name = "대회일 형식 오류", value = PayoutInternalApiExamples.INVALID_VALUE_TYPE)
			}))
	})
	public ResponseEntity<RsData<RefundEligibilityDto>> refundEligibility(
		@Parameter(description = "대회 ID. 주면 환불 마감일로 판단한다 (없으면 대회일 기준)", example = "10")
		@RequestParam(required = false) Integer raceId,
		@Parameter(description = "참가권의 대회일 (yyyy-MM-dd)", example = "2026-10-10")
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate raceDate
	) {
		RefundEligibilityDto eligibility = refundEligibilityUseCase.check(raceId, raceDate, LocalDate.now(ZONE));
		return ResponseEntity.ok(RsData.of(HttpStatus.OK, "환불 가능 여부를 조회했습니다.", eligibility));
	}
}
