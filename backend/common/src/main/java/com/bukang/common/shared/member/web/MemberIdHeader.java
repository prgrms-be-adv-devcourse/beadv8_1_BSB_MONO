package com.bukang.common.shared.member.web;

/**
 * 게이트웨이가 Access Token을 검사한 뒤 넘겨주는 회원 ID 헤더 (PRO-20).
 * 컨트롤러·예외 처리·Swagger 설정이 같은 이름·메시지를 쓰도록 모든 서비스가 이 상수를 쓴다.
 * gateway는 common에 의존하지 않아 JwtAuthenticationFilter에 같은 헤더 이름을 따로 둔다
 */
public final class MemberIdHeader {
	public static final String NAME = "X-Member-Id";
	public static final String INVALID_MESSAGE = "회원 ID가 올바르지 않습니다.";

	private MemberIdHeader() {
	}
}
