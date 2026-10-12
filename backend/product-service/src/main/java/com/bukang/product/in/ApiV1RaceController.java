package com.bukang.product.in;

import java.net.URI;
import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bukang.common.global.rsdata.RsData;
import com.bukang.product.app.ProductFacade;
import com.bukang.product.domain.RaceSaleStatus;
import com.bukang.product.dto.RaceSaveDto;
import com.bukang.product.dto.RaceSaveRequestDto;

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

// 게이트웨이가 경로 앞부분을 자르지 않으므로(StripPrefix 금지, PRO-17) 서비스 이름(/product)부터 쓴다
@Tag(name = "Race", description = "대회 등록, 수정, 판매 시작 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/product/races")
public class ApiV1RaceController {
	// 응답 메시지에 보여 주는 일시 형식 (예: 2026-10-13 10:00)
	private static final DateTimeFormatter MESSAGE_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private final ProductFacade productFacade;

	// ResponseEntity.created()의 201은 springdoc이 추론하지 못하므로 응답 코드를 직접 명시한다
	// 에러 응답은 GlobalExceptionHandler, ProductExceptionHandler가 RsData(data: null) 형식으로 반환한다
	@PostMapping
	@Operation(summary = "대회 등록",
		description = "승인된 판매자가 대회와 종목을 작성 중(DRAFT)으로 등록한다. 판매 시작 일시는 크루 예매 시작·개인 예매 오픈 중 "
			+ "이른 시각으로 채운다. 이미지는 종목마다 받고(대표 이미지 1장 필수), 등록이 커밋된 뒤 file-service에 연결을 요청한다. "
			+ "파일이 없거나 본인이 올린 파일이 아니어서 연결에 실패하면 응답에는 나오지 않고 file-service 로그에 남는다.")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "등록 성공",
			content = @Content(examples = @ExampleObject(value = RaceApiExamples.REGISTER_SUCCESS))),
		@ApiResponse(responseCode = "400",
			description = "입력값 검증 실패, 요청 본문 형식 오류, 날짜 순서·인원·나이·정원 오류, 이미지 용도·개수 오류, "
				+ "같은 파일 중복 또는 회원 ID 형식 오류",
			content = @Content(schema = @Schema(implementation = RsData.class), examples = {
				@ExampleObject(name = "입력값 검증 실패", value = RaceApiExamples.REGISTER_INVALID_INPUT),
				@ExampleObject(name = "요청 본문 형식 오류", value = RaceApiExamples.INVALID_BODY),
				@ExampleObject(name = "날짜 순서 오류", value = RaceApiExamples.INVALID_SCHEDULE),
				@ExampleObject(name = "대표 이미지 없음", value = RaceApiExamples.THUMBNAIL_REQUIRED),
				@ExampleObject(name = "같은 파일 중복", value = RaceApiExamples.DUPLICATE_FILE),
				@ExampleObject(name = "회원 ID 형식 오류", value = RaceApiExamples.INVALID_VALUE_TYPE)
			})),
		@ApiResponse(responseCode = "401", description = "회원 ID 헤더 없음",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = RaceApiExamples.LOGIN_REQUIRED))),
		@ApiResponse(responseCode = "403", description = "승인된 판매자가 아님",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = RaceApiExamples.SELLER_NOT_APPROVED)))
	})
	public ResponseEntity<RsData<RaceSaveDto>> register(
		@Parameter(description = "요청한 회원 ID (게이트웨이가 넣어 준다)", example = "9001")
		@RequestHeader("X-Member-Id") int memberId,
		@Valid @RequestBody RaceSaveRequestDto request
	) {
		RaceSaveDto race = productFacade.registerRace(memberId, request);
		String message = "대회가 작성 중으로 저장되었습니다. 판매 시작을 눌러야 %s부터 판매됩니다."
			.formatted(race.getSaleOpenAt().format(MESSAGE_DATE_TIME));
		return ResponseEntity.created(URI.create("/api/v1/product/races/" + race.getRaceId()))
			.body(RsData.of(HttpStatus.CREATED, message, race));
	}

	@PutMapping("/{raceId}")
	@Operation(summary = "대회 수정",
		description = "본인이 등록한 대회를 등록과 같은 모양의 요청으로 통째로 바꾼다. 종목은 courseId가 있으면 수정하고 없으면 추가하며, "
			+ "요청에 없는 기존 종목은 뺀다. 이미지 목록도 통째로 바뀌어, 수정이 커밋된 뒤 file-service에 다시 연결을 요청한다. "
			+ "판매가 시작된 뒤에는 참가비를 올리거나 환불 기준일을 앞당길 수 없고, 정원은 언제나 확보·판매된 자리보다 줄일 수 없다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "수정 성공",
			content = @Content(examples = @ExampleObject(value = RaceApiExamples.UPDATE_SUCCESS))),
		@ApiResponse(responseCode = "400",
			description = "입력값 검증 실패, 요청 본문 형식 오류, 날짜 순서·인원·나이·정원 오류, 이미지 용도·개수 오류, "
				+ "같은 파일 중복, 이 대회의 종목이 아닌 종목 ID 또는 대회 ID·회원 ID 형식 오류",
			content = @Content(schema = @Schema(implementation = RsData.class), examples = {
				@ExampleObject(name = "입력값 검증 실패", value = RaceApiExamples.REGISTER_INVALID_INPUT),
				@ExampleObject(name = "요청 본문 형식 오류", value = RaceApiExamples.INVALID_BODY),
				@ExampleObject(name = "날짜 순서 오류", value = RaceApiExamples.INVALID_SCHEDULE),
				@ExampleObject(name = "대표 이미지 없음", value = RaceApiExamples.THUMBNAIL_REQUIRED),
				@ExampleObject(name = "같은 파일 중복", value = RaceApiExamples.DUPLICATE_FILE),
				@ExampleObject(name = "이 대회의 종목이 아님", value = RaceApiExamples.COURSE_NOT_IN_RACE),
				@ExampleObject(name = "대회 ID·회원 ID 형식 오류", value = RaceApiExamples.INVALID_VALUE_TYPE)
			})),
		@ApiResponse(responseCode = "401", description = "회원 ID 헤더 없음",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = RaceApiExamples.LOGIN_REQUIRED))),
		@ApiResponse(responseCode = "403", description = "승인된 판매자가 아님 또는 다른 판매자의 대회",
			content = @Content(schema = @Schema(implementation = RsData.class), examples = {
				@ExampleObject(name = "승인된 판매자가 아님", value = RaceApiExamples.SELLER_NOT_APPROVED),
				@ExampleObject(name = "다른 판매자의 대회", value = RaceApiExamples.NOT_OWNER)
			})),
		@ApiResponse(responseCode = "404", description = "없는 대회",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = RaceApiExamples.RACE_NOT_FOUND))),
		@ApiResponse(responseCode = "409",
			description = "판매 시작 뒤 참가비 인상·환불 기준일 앞당김, 확보·판매된 자리보다 작은 정원 또는 자리가 있는 종목 빼기",
			content = @Content(schema = @Schema(implementation = RsData.class), examples = {
				@ExampleObject(name = "참가비 인상", value = RaceApiExamples.PRICE_RAISED),
				@ExampleObject(name = "환불 기준일 앞당김", value = RaceApiExamples.REFUND_DEADLINE_ADVANCED),
				@ExampleObject(name = "정원 축소", value = RaceApiExamples.QUOTA_BELOW_TAKEN),
				@ExampleObject(name = "자리가 있는 종목 빼기", value = RaceApiExamples.COURSE_HAS_PARTICIPANTS)
			}))
	})
	public ResponseEntity<RsData<RaceSaveDto>> update(
		@Parameter(description = "요청한 회원 ID (게이트웨이가 넣어 준다)", example = "9001")
		@RequestHeader("X-Member-Id") int memberId,
		@Parameter(description = "대회 ID", example = "1")
		@PathVariable int raceId,
		@Valid @RequestBody RaceSaveRequestDto request
	) {
		return ResponseEntity.ok()
			.body(RsData.of(HttpStatus.OK, "대회를 수정했습니다.", productFacade.updateRace(memberId, raceId, request)));
	}

	@PostMapping("/{raceId}/sale")
	@Operation(summary = "대회 판매 시작",
		description = "본인이 등록한 작성 중 대회의 판매를 시작한다. 판매 예정이 되어 판매 시작 일시부터 판매되고, "
			+ "판매 시작 일시가 이미 지났으면 바로 판매 중이 된다. 대회 검수는 없다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "판매 시작 성공",
			content = @Content(examples = {
				@ExampleObject(name = "판매 예정", value = RaceApiExamples.SALE_SCHEDULED),
				@ExampleObject(name = "판매 시작 일시가 지나 바로 판매 중", value = RaceApiExamples.SALE_STARTED)
			})),
		@ApiResponse(responseCode = "400", description = "대회 ID·회원 ID 형식 오류",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = RaceApiExamples.INVALID_VALUE_TYPE))),
		@ApiResponse(responseCode = "401", description = "회원 ID 헤더 없음",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = RaceApiExamples.LOGIN_REQUIRED))),
		@ApiResponse(responseCode = "403", description = "승인된 판매자가 아님 또는 다른 판매자의 대회",
			content = @Content(schema = @Schema(implementation = RsData.class), examples = {
				@ExampleObject(name = "승인된 판매자가 아님", value = RaceApiExamples.SELLER_NOT_APPROVED),
				@ExampleObject(name = "다른 판매자의 대회", value = RaceApiExamples.NOT_OWNER)
			})),
		@ApiResponse(responseCode = "404", description = "없는 대회",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = RaceApiExamples.RACE_NOT_FOUND))),
		@ApiResponse(responseCode = "409", description = "작성 중이 아닌 대회",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = RaceApiExamples.NOT_DRAFT)))
	})
	public ResponseEntity<RsData<RaceSaveDto>> startSale(
		@Parameter(description = "요청한 회원 ID (게이트웨이가 넣어 준다)", example = "9001")
		@RequestHeader("X-Member-Id") int memberId,
		@Parameter(description = "대회 ID", example = "1")
		@PathVariable int raceId
	) {
		RaceSaveDto race = productFacade.startRaceSale(memberId, raceId);
		String message = race.getSaleStatus() == RaceSaleStatus.ON_SALE
			? "판매를 시작했습니다."
			: "%s에 판매가 시작됩니다.".formatted(race.getSaleOpenAt().format(MESSAGE_DATE_TIME));
		return ResponseEntity.ok().body(RsData.of(HttpStatus.OK, message, race));
	}
}
