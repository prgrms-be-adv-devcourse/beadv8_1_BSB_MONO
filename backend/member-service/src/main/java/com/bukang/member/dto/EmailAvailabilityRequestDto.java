package com.bukang.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 쿼리 파라미터로 받는다. @RequestParam에 바로 검증을 걸면 공통 400 응답(INVALID_REQUEST)으로 이어지지 않아 DTO로 받는다
@Schema(description = "이메일 사용 가능 여부 확인 요청")
public record EmailAvailabilityRequestDto(
	@Schema(description = "확인할 이메일", example = "runner1@crewrun.com")
	@NotBlank(message = "이메일을 작성해주세요.")
	@Email(message = "이메일 형식에 맞게 작성해주세요.")
	@Size(max = 255, message = "이메일은 255자 이하로 작성해주세요.")
	String email
) {
}
