package com.bukang.payout.in;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bukang.common.global.rsdata.RsData;
import com.bukang.payout.app.PayoutRecoveryUseCase;
import com.bukang.payout.dto.PayoutRecoveryDto;

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
 * 관리자의 회수 완료 (판매자 입금 확인)
 * 회원 ID는 X-Member-Id 헤더를 그대로 믿는다. 관리자인지는 지급보류 API와 같이 게이트웨이가 막을 예정이다 (PRO-20)
 */
@Tag(name = "PayoutRecovery", description = "회수 완료 API (관리자)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payout")
public class ApiV1PayoutRecoveryController {
	// 서버 시간대와 상관없이 한국 시간으로 남긴다 (지급보류 API와 같은 기준)
	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	private final PayoutRecoveryUseCase payoutRecoveryUseCase;

	// 에러 응답은 GlobalExceptionHandler, PayoutExceptionHandler가 RsData(data: null) 형식으로 반환한다
	@PostMapping("/recoveries/{recoveryId}/complete")
	@Operation(summary = "회수 완료",
		description = "판매자의 입금을 확인한 회수를 회수 완료(COMPLETED)로 바꾸고 확인한 관리자와 일시를 남긴다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "완료 성공",
			content = @Content(examples = @ExampleObject(value = PayoutRecoveryApiExamples.COMPLETE_SUCCESS))),
		@ApiResponse(responseCode = "400", description = "회수 ID·회원 ID 형식 오류",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = PayoutApiExamples.INVALID_VALUE_TYPE))),
		@ApiResponse(responseCode = "401", description = "회원 ID 헤더 없음",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = PayoutApiExamples.LOGIN_REQUIRED))),
		@ApiResponse(responseCode = "404", description = "없는 회수",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = PayoutRecoveryApiExamples.RECOVERY_NOT_FOUND))),
		@ApiResponse(responseCode = "409", description = "이미 완료됨",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = PayoutRecoveryApiExamples.ALREADY_COMPLETED)))
	})
	public ResponseEntity<RsData<PayoutRecoveryDto>> complete(
		@Parameter(description = "요청한 관리자 회원 ID (게이트웨이가 넣어 줄 예정)", example = "1")
		@RequestHeader("X-Member-Id") int memberId,
		@Parameter(description = "회수 ID", example = "1")
		@PathVariable int recoveryId
	) {
		PayoutRecoveryDto recovery = payoutRecoveryUseCase.complete(recoveryId, memberId, LocalDateTime.now(ZONE));
		return ResponseEntity.ok(RsData.of(HttpStatus.OK, "회수를 완료했습니다.", recovery));
	}
}
