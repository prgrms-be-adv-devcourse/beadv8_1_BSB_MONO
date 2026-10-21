package com.bukang.member.in;

/**
 * 내부 API(InternalV1MemberController)의 Swagger 응답 예시
 */
final class MemberInternalApiExamples {
	static final String ADMIN = """
		{ "status": 200, "message": "관리자 여부를 조회했습니다.", "data": { "admin": true } }
		""";

	static final String NOT_ADMIN = """
		{ "status": 200, "message": "관리자 여부를 조회했습니다.", "data": { "admin": false } }
		""";

	static final String MEMBER_NOT_FOUND = """
		{ "status": 404, "code": "MEMBER_NOT_FOUND", "message": "존재하지 않는 회원입니다.", "data": null }
		""";

	private MemberInternalApiExamples() {
	}
}
