package com.bukang.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 재발급 응답")
public record TokenReissueResponseDto(
	@Schema(description = "새 Access Token. 인증이 필요한 요청에 Authorization: Bearer {accessToken} 헤더로 보낸다")
	String accessToken
) {
}
