package com.bukang.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 사용 가능 여부 응답")
public record EmailAvailabilityResponseDto(
	@Schema(description = "사용 가능하면 true, 이미 가입된 이메일이면 false", example = "true")
	boolean available
) {
}
