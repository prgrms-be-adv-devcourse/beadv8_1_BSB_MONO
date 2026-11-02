package com.bukang.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "계정 연결 표")
public record SocialLinkTicketDto(
	@Schema(description = "2분 동안 한 번만 쓸 수 있는 연결 표", example = "8f1c2a9e4b7d4c3a9f0e6b5d2c1a7e3f")
	String linkTicket
) {
}
