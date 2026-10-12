package com.bukang.order.in;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bukang.common.global.rsdata.RsData;
import com.bukang.order.app.OrderCreateResult;
import com.bukang.order.app.OrderFacade;
import com.bukang.order.dto.OrderCreateRequestDto;
import com.bukang.order.dto.OrderDto;

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
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@Tag(name = "Order", description = "바로 결제 예매(주문 생성·자리 임시 확보) API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/order/orders")
public class ApiV1OrderController {
	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

	private final OrderFacade orderFacade;

	// 201·200·409(매진)를 직접 고르므로 응답 코드를 모두 명시한다
	// 에러 응답은 GlobalExceptionHandler, OrderExceptionHandler가 RsData(data: null) 형식으로 반환한다
	@PostMapping
	@Operation(summary = "바로 결제 예매 ([결제하기])",
		description = "자리 확보 전(PENDING) 참가 신청으로 상품 서비스에 자리 1석을 확보받고, 결제 기한 10분짜리 주문(CREATED)을 만든다. "
			+ "같은 Idempotency-Key로 다시 보내면 새 주문 없이 처음 주문을 돌려준다. "
			+ "남은 자리가 없으면 참가 신청이 확보 실패(FAILED)가 되고 주문은 만들지 않는다.")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "자리 확보, 주문 생성",
			content = @Content(examples = @ExampleObject(value = OrderApiExamples.CREATE_SUCCESS))),
		@ApiResponse(responseCode = "200", description = "같은 Idempotency-Key로 이미 만든 주문",
			content = @Content(examples = @ExampleObject(value = OrderApiExamples.CREATE_EXISTING))),
		@ApiResponse(responseCode = "400", description = "입력값·헤더 오류",
			content = @Content(schema = @Schema(implementation = RsData.class), examples = {
				@ExampleObject(name = "입력값 검증 실패", value = OrderApiExamples.INVALID_INPUT),
				@ExampleObject(name = "요청 본문 형식 오류", value = OrderApiExamples.INVALID_BODY),
				@ExampleObject(name = "Idempotency-Key 없음", value = OrderApiExamples.MISSING_IDEMPOTENCY_KEY),
				@ExampleObject(name = "Idempotency-Key 길이 오류", value = OrderApiExamples.INVALID_IDEMPOTENCY_KEY),
				@ExampleObject(name = "회원 ID 형식 오류", value = OrderApiExamples.INVALID_VALUE_TYPE),
				@ExampleObject(name = "회원 ID가 0 이하", value = OrderApiExamples.INVALID_MEMBER_ID)
			})),
		@ApiResponse(responseCode = "401", description = "회원 ID 헤더 없음",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = OrderApiExamples.LOGIN_REQUIRED))),
		@ApiResponse(responseCode = "403", description = "다른 회원의 참가 신청",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = OrderApiExamples.NOT_OWNER))),
		@ApiResponse(responseCode = "404", description = "없는 참가 신청 또는 종목",
			content = @Content(schema = @Schema(implementation = RsData.class), examples = {
				@ExampleObject(name = "없는 참가 신청", value = OrderApiExamples.PARTICIPATION_NOT_FOUND),
				@ExampleObject(name = "없는 종목", value = OrderApiExamples.COURSE_NOT_FOUND)
			})),
		@ApiResponse(responseCode = "409", description = "매진, 결제할 수 없는 상태, 접수 기간 아님, 요청 겹침, 키 재사용",
			content = @Content(schema = @Schema(implementation = RsData.class), examples = {
				@ExampleObject(name = "매진", value = OrderApiExamples.SOLD_OUT),
				@ExampleObject(name = "결제할 수 없는 상태", value = OrderApiExamples.NOT_PAYABLE),
				@ExampleObject(name = "접수 기간 아님", value = OrderApiExamples.NOT_ACCEPTING),
				@ExampleObject(name = "요청 겹침", value = OrderApiExamples.CONFLICT),
				@ExampleObject(name = "다른 신청서에 쓴 키", value = OrderApiExamples.KEY_REUSED)
			}))
	})
	public ResponseEntity<RsData<OrderDto>> create(
		@Parameter(description = "요청한 회원 ID (게이트웨이가 넣어 줄 예정)", example = "1")
		@RequestHeader(MemberIdHeader.NAME) @Positive(message = MemberIdHeader.INVALID_MESSAGE) int memberId,
		@Parameter(description = "중복 주문 방지 키. 같은 [결제하기] 재시도에는 같은 값을 보낸다 (예: UUID)",
			example = "6f1c2a7e-3b9d-4f0a-9c55-0d2e8b7a1f43")
		@RequestHeader(IDEMPOTENCY_KEY_HEADER) @Size(min = 1, max = 64, message = "Idempotency-Key는 1~64자로 보내주세요.")
		String idempotencyKey,
		@Valid @RequestBody OrderCreateRequestDto request
	) {
		OrderCreateResult result = orderFacade.create(request, idempotencyKey, memberId);
		return switch (result.outcome()) {
			case CREATED -> ResponseEntity.created(URI.create("/api/v1/order/orders/" + result.order().getId()))
				.body(RsData.of(HttpStatus.CREATED, "자리를 확보했습니다. 결제 기한 안에 결제해주세요.", result.order()));
			case EXISTING -> ResponseEntity.ok()
				.body(RsData.of(HttpStatus.OK, "이미 처리한 주문 요청입니다.", result.order()));
			case SOLD_OUT -> ResponseEntity.status(HttpStatus.CONFLICT)
				.body(RsData.of(HttpStatus.CONFLICT, "남은 자리가 없습니다. 참가 신청서를 다시 작성해 신청할 수 있습니다.", null));
		};
	}
}
