package com.bukang.product.in;

import java.net.URI;
import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bukang.common.global.rsdata.RsData;
import com.bukang.product.app.ProductFacade;
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
@Tag(name = "Race", description = "대회 등록 API")
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
			+ "이른 시각으로 채운다. 대회 이미지(대표 이미지 1장 필수)와 종목 이미지는 등록이 커밋된 뒤 file-service에 연결을 요청한다. "
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
}
