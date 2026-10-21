package com.bukang.member.in;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bukang.common.global.rsdata.RsData;
import com.bukang.member.app.SellerFacade;
import com.bukang.member.dto.MemberSellerDto;

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
 * 다른 서비스가 동기 호출로 부르는 내부 API. /internal/** 은 게이트웨이에 노출하지 않는다 (order-service와 같은 방식)
 * 회원이 아니라 서비스가 부르므로 회원 ID 헤더를 받지 않는다
 */
@Tag(name = "MemberInternal", description = "회원의 판매자 ID 조회 API (서비스 간 내부 호출 전용)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/member")
public class InternalV1MemberController {
	private final SellerFacade sellerFacade;

	@GetMapping("/members/{memberId}/seller")
	@Operation(summary = "회원의 판매자 ID 조회",
		description = "정산 서비스가 판매자 정산 조회 때 회원 ID(X-Member-Id)를 판매자 ID(판매자 신청서 ID)로 바꾸려고 부른다. "
			+ "회원의 가장 최근 판매자 신청의 ID와 상태를 돌려주고, 신청한 적이 없으면 두 칸 모두 null이다. "
			+ "승인 여부는 부르는 쪽이 status로 판단한다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공 (신청이 없으면 sellerId·status가 null)",
			content = @Content(examples = {
				@ExampleObject(name = "승인된 판매자", value = MemberInternalApiExamples.SELLER_APPROVED),
				@ExampleObject(name = "대기 중인 신청", value = MemberInternalApiExamples.SELLER_PENDING),
				@ExampleObject(name = "신청 없음", value = MemberInternalApiExamples.SELLER_NONE)
			})),
		@ApiResponse(responseCode = "404", description = "존재하지 않거나 탈퇴한 회원",
			content = @Content(schema = @Schema(implementation = RsData.class),
				examples = @ExampleObject(value = MemberInternalApiExamples.MEMBER_NOT_FOUND)))
	})
	public ResponseEntity<RsData<MemberSellerDto>> findSellerOfMember(
		@Parameter(description = "회원 ID", example = "7") @PathVariable int memberId
	) {
		return ResponseEntity.ok()
			.body(RsData.of(HttpStatus.OK, "회원의 판매자 신청을 조회했습니다.", sellerFacade.findSellerOfMember(memberId)));
	}
}
