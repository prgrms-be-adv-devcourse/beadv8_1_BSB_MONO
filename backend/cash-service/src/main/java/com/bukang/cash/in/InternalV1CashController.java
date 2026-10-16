package com.bukang.cash.in;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.bukang.cash.app.CashFacade;
import com.bukang.cash.dto.CashCancelPaymentRequestDto;
import com.bukang.cash.dto.CashPaymentResponseDto;
import com.bukang.cash.dto.CashPaymentRequestDto;
import com.bukang.cash.dto.CashRefundResponseDto;
import com.bukang.cash.dto.CashRefundRequestDto;
import com.bukang.common.global.rsdata.RsData;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 서비스끼리 부르는 내부 API. 게이트웨이에 라우팅하지 않는다(/internal/** 경로)
 * 회원이 아니라 서비스가 부르므로 회원 ID 헤더를 받지 않는다 (file-service, order-service와 같은 방식)
 */
@Tag(name = "CashInternal", description = "주문 결제, 결제 취소(환원), 환불 API (서비스 간 내부 호출 전용)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/cash")
public class InternalV1CashController {
	private final CashFacade cashFacade;

	// 에러 응답은 GlobalExceptionHandler가 RsData(data: null) 형식으로 반환한다
	@PostMapping("/payments/{orderId}")
	@Operation(summary = "주문 결제", description = "주문 ID로 결제 행을 멱등하게 찾거나 만들고, 예치금 잔액에서 결제 금액을 차감한다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "결제 성공 또는 잔액 부족",
			content = @Content(examples = {
				@ExampleObject(name = "결제 성공", value = CashInternalApiExamples.PAY_SUCCESS_PAID),
				@ExampleObject(name = "잔액 부족", value = CashInternalApiExamples.PAY_SUCCESS_INSUFFICIENT)
			})),
		@ApiResponse(responseCode = "400", description = "입력값 검증 실패 또는 요청 본문 형식 오류",
			content = @Content(schema = @Schema(implementation = RsData.class), examples = {
				@ExampleObject(name = "입력값 검증 실패", value = CashInternalApiExamples.INVALID_MEMBER_ID),
				@ExampleObject(name = "요청 본문 형식 오류", value = CashInternalApiExamples.INVALID_BODY)
			})),
		@ApiResponse(responseCode = "404", description = "존재하지 않는 지갑",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = CashInternalApiExamples.WALLET_NOT_FOUND))),
		@ApiResponse(responseCode = "409", description = "이미 취소·환불된 결제 또는 결제한 회원과 요청한 회원이 다름",
			content = @Content(schema = @Schema(implementation = RsData.class), examples = {
				@ExampleObject(name = "이미 취소·환불됨", value = CashInternalApiExamples.PAYMENT_ALREADY_FINALIZED),
				@ExampleObject(name = "결제 회원 불일치", value = CashInternalApiExamples.PAYMENT_MEMBER_MISMATCH)
			}))
	})
	public ResponseEntity<RsData<CashPaymentResponseDto>> pay(
		@Parameter(description = "주문 ID", example = "100") @PathVariable int orderId,
		@Valid @RequestBody CashPaymentRequestDto request
	) {
		CashPaymentResponseDto result = cashFacade.completeOrderPayment(orderId, request.getMemberId(), request.getAmount());
		return ResponseEntity.ok().body(RsData.of(HttpStatus.OK, result.message(), result));
	}


	@PostMapping("/payments/{orderId}/cancel")
	@Operation(summary = "결제 취소(환원)", description = "결제 완료된 주문이면 예치금을 돌려주고, 결제 전이면 뒤늦게 오는 결제를 "
		+ "거절하도록 선점한다. 이미 취소·환불됐으면 아무것도 바꾸지 않고 성공한다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "취소 성공",
			content = @Content(examples = @ExampleObject(value = CashInternalApiExamples.CANCEL_SUCCESS))),
		@ApiResponse(responseCode = "400", description = "입력값 검증 실패 또는 요청 본문 형식 오류",
			content = @Content(schema = @Schema(implementation = RsData.class), examples = {
				@ExampleObject(name = "입력값 검증 실패", value = CashInternalApiExamples.INVALID_MEMBER_ID),
				@ExampleObject(name = "요청 본문 형식 오류", value = CashInternalApiExamples.INVALID_BODY)
			})),
		@ApiResponse(responseCode = "404", description = "존재하지 않는 지갑",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = CashInternalApiExamples.WALLET_NOT_FOUND))),
		@ApiResponse(responseCode = "409", description = "결제한 회원과 요청한 회원이 다름",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = CashInternalApiExamples.PAYMENT_MEMBER_MISMATCH)))
	})
	public ResponseEntity<RsData<Void>> cancelPayment(
		@Parameter(description = "주문 ID", example = "100") @PathVariable int orderId,
		@Valid @RequestBody CashCancelPaymentRequestDto request
	) {
		cashFacade.cancelPayment(orderId, request.getMemberId());
		return ResponseEntity.ok().body(RsData.of(HttpStatus.OK, "결제를 취소했습니다.", null));
	}

	@PostMapping("/payments/{orderId}/refund")
	@Operation(summary = "예치금 환불", description = "결제 완료된 주문만 환불한다. 이미 환원·환불됐으면 그대로 성공하고, "
		+ "결제된 적 없는 주문은 거절한다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "환불 성공",
			content = @Content(examples = @ExampleObject(value = CashInternalApiExamples.REFUND_SUCCESS))),
		@ApiResponse(responseCode = "400", description = "입력값 검증 실패 또는 요청 본문 형식 오류",
			content = @Content(schema = @Schema(implementation = RsData.class), examples = {
				@ExampleObject(name = "입력값 검증 실패", value = CashInternalApiExamples.INVALID_MEMBER_ID),
				@ExampleObject(name = "요청 본문 형식 오류", value = CashInternalApiExamples.INVALID_BODY)
			})),
		@ApiResponse(responseCode = "404", description = "존재하지 않는 지갑",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = CashInternalApiExamples.WALLET_NOT_FOUND))),
		@ApiResponse(responseCode = "409", description = "환불할 결제가 없음, 결제가 완료되지 않음 또는 결제한 회원과 요청한 회원이 다름",
			content = @Content(schema = @Schema(implementation = RsData.class), examples = {
				@ExampleObject(name = "환불할 결제 없음", value = CashInternalApiExamples.REFUND_NOT_FOUND),
				@ExampleObject(name = "결제 미완료", value = CashInternalApiExamples.REFUND_NOT_COMPLETED),
				@ExampleObject(name = "결제 회원 불일치", value = CashInternalApiExamples.PAYMENT_MEMBER_MISMATCH)
			}))
	})
	public ResponseEntity<RsData<CashRefundResponseDto>> refund(
		@Parameter(description = "주문 ID", example = "100") @PathVariable int orderId,
		@Valid @RequestBody CashRefundRequestDto request
	) {
		CashRefundResponseDto result = cashFacade.refund(orderId, request.getMemberId());
		return ResponseEntity.ok().body(RsData.of(HttpStatus.OK, "환불했습니다.", result));
	}
}
