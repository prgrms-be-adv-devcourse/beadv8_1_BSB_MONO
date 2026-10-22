package com.bukang.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 여부 응답 (서비스 간 내부 호출용)")
public record MemberAdminDto(
	@Schema(description = "역할이 관리자면 true, 일반 회원이면 false", example = "true")
	boolean admin
) {
}
