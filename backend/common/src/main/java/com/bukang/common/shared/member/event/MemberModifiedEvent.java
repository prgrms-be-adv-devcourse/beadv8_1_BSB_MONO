package com.bukang.common.shared.member.event;

import java.time.LocalDateTime;

public record MemberModifiedEvent(
	int id,
	LocalDateTime createDate,
	LocalDateTime modifyDate,
	String username,
	String nickname
) {
}
