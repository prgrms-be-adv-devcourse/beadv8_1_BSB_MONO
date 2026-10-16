package com.bukang.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 칸이 하나라 생성자 하나로 받으면 JSON 객체가 아니라 값 자체로 읽힐 수 있어, 기본 생성자 + 필드로 받는다
@Schema(description = "판매자 신청 반려 요청")
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SellerRejectRequestDto {
	@Schema(description = "반려 사유 (500자 이하, 신청자에게 보여 준다)", example = "사업자등록번호를 확인할 수 없습니다.")
	@NotBlank(message = "반려 사유를 작성해주세요.")
	@Size(max = 500, message = "반려 사유는 500자 이하로 작성해주세요.")
	private String reason;
}
