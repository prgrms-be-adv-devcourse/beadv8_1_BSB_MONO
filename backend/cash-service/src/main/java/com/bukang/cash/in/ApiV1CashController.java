package com.bukang.cash.in;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bukang.cash.app.CashFacade;
import com.bukang.cash.dto.WalletBalanceDto;
import com.bukang.common.global.rsdata.RsData;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
	public ResponseEntity<RsData<WalletBalanceDto>> getBalance(
		@Parameter(description = "지갑 주인의 회원 ID", example = "3") @PathVariable int holderId
	) {
		WalletBalanceDto walletBalanceDto = cashFacade.getBalance(holderId);
		return ResponseEntity.ok(RsData.of(HttpStatus.OK, "잔액 조회 성공", walletBalanceDto));
	}
}
