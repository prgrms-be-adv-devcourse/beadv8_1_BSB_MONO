package com.bukang.member.in;

/**
 * 게이트웨이가 Access Token을 검사한 뒤 넘겨주는 회원 ID 헤더 (PRO-20). 컨트롤러·예외 처리·Swagger 설정이 같은 이름을 쓰도록 모아 둔다
 */
public final class MemberIdHeader {
	public static final String NAME = "X-Member-Id";

	private MemberIdHeader() {
	}
}
