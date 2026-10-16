package com.bukang.order.dto;

/**
 * 끝나지 않은 참가권 수 (회원 탈퇴 확인용 내부 응답, BE-022)
 */
public record ActiveTicketCountDto(
	int count
) {
}
