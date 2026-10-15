package com.bukang.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답")
public record MemberLoginResponseDto(
	@Schema(description = "Access Token. 인증이 필요한 요청에 Authorization: Bearer {accessToken} 헤더로 보낸다")
	String accessToken,

	@Schema(description = "로그인한 회원")
	MemberDto member
) {
}
