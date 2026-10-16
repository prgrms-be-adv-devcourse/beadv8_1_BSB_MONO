package com.bukang.cash.in;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bukang.cash.app.CashFacade;
import com.bukang.cash.dto.CashChargeConfirmRequestDto;
import com.bukang.cash.dto.CashChargeResponseDto;
import com.bukang.cash.dto.WalletBalanceResponseDto;
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
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Tag(name = "Cash", description = "예치금 지갑 잔액 조회 API")
@RestController
@RequestMapping("/api/v1/cash")
@RequiredArgsConstructor
public class ApiV1CashController {
	private final CashFacade cashFacade;

	// 에러 응답은 GlobalExceptionHandler가 RsData(data: null) 형식으로 반환한다
	// JWT 인증이 생기기 전까지는 회원 ID(= 지갑 ID)를 경로로 받는다
	@GetMapping("/wallets/{holderId}/balance")
	@Operation(summary = "지갑 잔액 조회", description = "회원 ID로 지갑을 찾아 현재 예치금 잔액을 조회한다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공",
			content = @Content(examples = @ExampleObject(value = CashApiExamples.GET_BALANCE_SUCCESS))),
		@ApiResponse(responseCode = "404", description = "지갑 없음 (회원 가입 이벤트가 아직 처리되지 않았거나 없는 회원)",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = CashApiExamples.WALLET_NOT_FOUND)))
	})
	public ResponseEntity<RsData<WalletBalanceResponseDto>> getBalance(
		@Parameter(description = "지갑 주인의 회원 ID", example = "3") @PathVariable int holderId
	) {
		WalletBalanceResponseDto walletBalanceResponseDto = cashFacade.getBalance(holderId);
		return ResponseEntity.ok(RsData.of(HttpStatus.OK, "잔액 조회 성공", walletBalanceResponseDto));
	}

	// 에러 응답은 GlobalExceptionHandler가 RsData(data: null) 형식으로 반환한다
	// PG 승인이 거절돼도 예외가 아니라 200으로 응답한다 (data.outcome이 FAILED)
	@PostMapping("/charges/confirm")
	@Operation(summary = "충전 승인 확정", description = "프론트가 Toss 결제위젯 콜백으로 받은 paymentKey·orderId·amount로 "
		+ "PG 승인을 확정하고, 성공하면 예치금 지갑에 반영한다. orderId를 멱등키로 써서 같은 요청이 여러 번 와도 한 번만 처리한다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "충전 확정 처리됨 (성공 또는 PG 승인 거절)",
			content = @Content(examples = {
				@ExampleObject(name = "충전 성공", value = CashApiExamples.CHARGE_CONFIRM_SUCCESS),
				@ExampleObject(name = "PG 승인 거절", value = CashApiExamples.CHARGE_CONFIRM_FAILED)
			})),
		@ApiResponse(responseCode = "400", description = "최소 충전 금액 미만, 입력값 검증 실패 또는 요청 본문 형식 오류",
			content = @Content(schema = @Schema(implementation = RsData.class), examples = {
				@ExampleObject(name = "최소 충전 금액 미만", value = CashApiExamples.CHARGE_AMOUNT_TOO_SMALL),
				@ExampleObject(name = "입력값 검증 실패", value = CashApiExamples.CHARGE_CONFIRM_INVALID_INPUT),
				@ExampleObject(name = "요청 본문 형식 오류", value = CashApiExamples.INVALID_BODY)
			})),
		@ApiResponse(responseCode = "404", description = "지갑 없음 (회원 가입 이벤트가 아직 처리되지 않았거나 없는 회원)",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = CashApiExamples.WALLET_NOT_FOUND)))
	})
	public ResponseEntity<RsData<CashChargeResponseDto>> confirmWalletCharge(
		@Parameter(description = "충전할 회원 ID (게이트웨이가 Access Token에서 넣어준다)", example = "3")
		@RequestHeader(MemberIdHeader.NAME) @Positive(message = MemberIdHeader.INVALID_MESSAGE) int memberId,
		@Valid @RequestBody CashChargeConfirmRequestDto request
	) {
		CashChargeResponseDto response = cashFacade.confirmWalletCharge(
			memberId, request.getPaymentKey(), request.getOrderId(), request.getAmount());
		return ResponseEntity.ok(RsData.of(HttpStatus.OK, response.message(), response));
	}
}
