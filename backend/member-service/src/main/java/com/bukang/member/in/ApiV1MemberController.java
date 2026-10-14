package com.bukang.member.in;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bukang.common.global.rsdata.RsData;
import com.bukang.member.app.MemberFacade;
import com.bukang.member.dto.MemberDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Member", description = "내 정보 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/member/members")
public class ApiV1MemberController {
	private final MemberFacade memberFacade;

	// 새로고침 등으로 화면이 로그인한 회원 정보를 잃었을 때 Access Token만으로 다시 불러오는 용도
	@GetMapping("/me")
	@Operation(summary = "내 정보 조회",
		description = "게이트웨이가 Access Token에서 꺼내 넣은 회원 ID(X-Member-Id 헤더)로 로그인한 회원의 정보를 조회한다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공",
			content = @Content(examples = @ExampleObject(value = MemberApiExamples.FIND_ME_SUCCESS))),
		@ApiResponse(responseCode = "401", description = "회원 ID 헤더 없음 (로그인하지 않은 요청)",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = MemberApiExamples.LOGIN_REQUIRED))),
		@ApiResponse(responseCode = "404", description = "헤더의 회원이 없음",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = MemberApiExamples.MEMBER_NOT_FOUND)))
	})
	public ResponseEntity<RsData<MemberDto>> findMe(
		@Parameter(description = "로그인한 회원 ID (게이트웨이가 Access Token에서 꺼내 넣는다)", example = "1")
		@RequestHeader(MemberIdHeader.NAME) int memberId
	) {
		return ResponseEntity.ok()
			.body(RsData.of(HttpStatus.OK, "내 정보를 조회했습니다.", memberFacade.findMe(memberId)));
	}
}
