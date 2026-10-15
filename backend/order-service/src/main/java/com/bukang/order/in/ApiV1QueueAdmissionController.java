package com.bukang.order.in;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bukang.common.global.rsdata.RsData;
import com.bukang.order.app.QueueAdmissionUseCase;
import com.bukang.order.dto.QueueAdmissionDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Tag(name = "Queue", description = "결제 페이지 타이머용 대기열 입장 상태 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/order/queue")
public class ApiV1QueueAdmissionController {
	private final QueueAdmissionUseCase queueAdmissionUseCase;

	@GetMapping("/admission")
	@Operation(summary = "대기열 입장 상태 (결제 페이지 타이머)",
		description = "이 종목에 대기열로 입장 중인지와 입장 끝 시각, 남은 초를 돌려준다. 입장 끝 시각이 [결제하기]의 결제 기한이다. "
			+ "타이머는 remainingSeconds로 센다(서버 시각 기준). 입장 중이 아니면 admitted=false이고 [결제하기]가 403이다. "
			+ "대기열 API(PRO-182)가 나오기 전에는 가짜 대기열이 답한다: 결제 페이지를 처음 열 때 입장해 10분 뒤 끝나고, "
			+ "끝난 뒤 이 API를 다시 부르면(페이지를 다시 열면) 다시 입장한다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공",
			content = @Content(examples = {
				@ExampleObject(name = "입장 중", value = QueueApiExamples.ADMITTED),
				@ExampleObject(name = "입장 중 아님", value = QueueApiExamples.NOT_ADMITTED)
			})),
		@ApiResponse(responseCode = "400", description = "회원 ID·대회 ID·종목 ID 오류",
			content = @Content(schema = @Schema(implementation = RsData.class), examples = {
				@ExampleObject(name = "회원 ID가 0 이하", value = QueueApiExamples.INVALID_MEMBER_ID),
				@ExampleObject(name = "대회 ID가 0 이하", value = QueueApiExamples.INVALID_RACE_ID)
			})),
		@ApiResponse(responseCode = "401", description = "회원 ID 헤더 없음",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = QueueApiExamples.LOGIN_REQUIRED)))
	})
	public ResponseEntity<RsData<QueueAdmissionDto>> findAdmission(
		@Parameter(description = "요청한 회원 ID (게이트웨이가 넣어 줄 예정)", example = "1")
		@RequestHeader(MemberIdHeader.NAME) @Positive(message = MemberIdHeader.INVALID_MESSAGE) int memberId,
		@Parameter(description = "대회 ID", example = "1")
		@RequestParam @Positive(message = "대회 ID가 올바르지 않습니다.") int raceId,
		@Parameter(description = "종목 ID", example = "1")
		@RequestParam @Positive(message = "종목 ID가 올바르지 않습니다.") int raceCourseId
	) {
		QueueAdmissionDto admission = queueAdmissionUseCase.find(memberId, raceId, raceCourseId, LocalDateTime.now());
		String message = admission.isAdmitted() ? "대기열에 입장 중입니다." : "대기열에 입장 중이 아닙니다.";
		return ResponseEntity.ok().body(RsData.of(HttpStatus.OK, message, admission));
	}
}
