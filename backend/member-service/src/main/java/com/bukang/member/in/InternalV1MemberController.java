package com.bukang.member.in;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bukang.common.global.rsdata.RsData;
import com.bukang.member.app.MemberFacade;
import com.bukang.member.dto.MemberAdminDto;

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
 * 다른 서비스가 동기 호출로 부르는 내부 API. /internal/** 은 게이트웨이에 노출하지 않는다 (order·file-service와 같은 방식)
 * 회원이 아니라 서비스가 부르므로 회원 ID 헤더를 받지 않는다
 */
@Tag(name = "MemberInternal", description = "관리자 여부 조회 API (서비스 간 내부 호출 전용)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/member")
public class InternalV1MemberController {
	private final MemberFacade memberFacade;

	@GetMapping("/members/{memberId}/admin")
	@Operation(summary = "관리자 여부 조회",
		description = "정산 서비스가 관리자 API(지급보류 걸기·풀기, 회수 완료)를 받을 때 부른다. "
			+ "회원 역할이 관리자면 admin=true, 일반 회원이면 false를 돌려준다. 관리자가 아니어도 200이며, 403은 부른 서비스가 정한다. "
			+ "탈퇴한 회원은 없는 회원으로 본다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공 (일반 회원이어도 200)",
			content = @Content(examples = {
				@ExampleObject(name = "관리자", value = MemberInternalApiExamples.ADMIN),
				@ExampleObject(name = "일반 회원", value = MemberInternalApiExamples.NOT_ADMIN)
			})),
		@ApiResponse(responseCode = "404", description = "없거나 탈퇴한 회원",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = MemberInternalApiExamples.MEMBER_NOT_FOUND)))
	})
	public ResponseEntity<RsData<MemberAdminDto>> checkAdmin(
		@Parameter(description = "확인할 회원 ID (관리자 API를 부른 회원)", example = "1")
		@PathVariable int memberId
	) {
		return ResponseEntity.ok()
			.body(RsData.of(HttpStatus.OK, "관리자 여부를 조회했습니다.", memberFacade.checkAdmin(memberId)));
	}
}
