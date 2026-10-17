package com.bukang.file.in;

/**
 * 게이트웨이가 넘겨주는 회원 ID 헤더. 컨트롤러와 예외 처리가 같은 이름·메시지를 쓰도록 모아 둔다
 * (order-service·cash-service·queue-service·product-service의 MemberIdHeader와 같은 이름·메시지)
 */
final class MemberIdHeader {
	static final String NAME = "X-Member-Id";
	static final String INVALID_MESSAGE = "회원 ID가 올바르지 않습니다.";

	private MemberIdHeader() {
	}
}
