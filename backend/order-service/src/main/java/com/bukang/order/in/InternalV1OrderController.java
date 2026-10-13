package com.bukang.order.in;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bukang.common.global.rsdata.RsData;
import com.bukang.order.app.SoldEntryQueryUseCase;
import com.bukang.order.dto.SoldEntryDto;

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
 * 다른 서비스가 동기 호출로 부르는 내부 API. /internal/** 은 게이트웨이에 노출하지 않는다 (file-service와 같은 방식)
 * 회원이 아니라 서비스가 부르므로 회원 ID 헤더를 받지 않는다
 */
@Tag(name = "OrderInternal", description = "정산용 판매 내역 조회 API (서비스 간 내부 호출 전용)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/order")
public class InternalV1OrderController {
	private final SoldEntryQueryUseCase soldEntryQueryUseCase;

	@GetMapping("/sold-entries")
	@Operation(summary = "정산용 판매 내역 조회",
		description = "정산 서비스가 정산 대상일 배치에서 부른다. from ~ to(양 끝 포함, 최대 31일)에 열린 대회의 주문 상품 중 "
			+ "결제 완료이고 취소되지 않은 참가권을 주문 상품 ID 순으로 돌려준다. 정산 배치는 놓친 날을 따라잡으려고 7일치를 묻는다. "
			+ "필드는 정산 서비스의 SoldEntry와 같고, raceDate는 주문 때 복사한 대회 날짜, price는 1장 값(키트 포함)이다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공 (없으면 빈 목록)",
			content = @Content(examples = {
				@ExampleObject(name = "판매 내역 있음", value = OrderInternalApiExamples.SOLD_ENTRIES_SUCCESS),
				@ExampleObject(name = "판매 내역 없음", value = OrderInternalApiExamples.SOLD_ENTRIES_EMPTY)
			})),
		@ApiResponse(responseCode = "400", description = "기간 없음, 형식 오류, 시작일 > 종료일, 31일 초과",
			content = @Content(schema = @Schema(implementation = RsData.class), examples = {
				@ExampleObject(name = "기간 없음", value = OrderInternalApiExamples.MISSING_FROM),
				@ExampleObject(name = "날짜 형식 오류", value = OrderInternalApiExamples.INVALID_VALUE_TYPE),
				@ExampleObject(name = "시작일이 종료일보다 늦음", value = OrderInternalApiExamples.FROM_AFTER_TO),
				@ExampleObject(name = "31일 초과", value = OrderInternalApiExamples.PERIOD_TOO_LONG)
			}))
	})
	public ResponseEntity<RsData<List<SoldEntryDto>>> findSoldEntries(
		@Parameter(description = "조회 시작 대회 날짜 (yyyy-MM-dd, 포함)", example = "2026-11-09")
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@Parameter(description = "조회 끝 대회 날짜 (yyyy-MM-dd, 포함)", example = "2026-11-15")
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
	) {
		return ResponseEntity.ok()
			.body(RsData.of(HttpStatus.OK, "판매 내역을 조회했습니다.", soldEntryQueryUseCase.findSoldEntries(from, to)));
	}
}
