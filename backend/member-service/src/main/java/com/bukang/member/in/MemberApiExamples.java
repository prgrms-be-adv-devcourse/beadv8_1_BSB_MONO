package com.bukang.member.in;

/**
 * ApiV1MemberController의 Swagger 응답 예시 (실제 응답 메시지와 같게 유지한다)
 */
final class MemberApiExamples {
	static final String FIND_ME_SUCCESS = """
		{
			"status": 200,
			"message": "내 정보를 조회했습니다.",
			"data": {
				"id": 1,
				"createDate": "2026-10-05T21:25:35",
				"modifyDate": "2026-10-05T21:25:35",
				"nickname": "러너1",
				"email": "runner1@crewrun.com",
				"phone": "010-1234-5678"
			}
		}
		""";

	static final String LOGIN_REQUIRED = """
		{ "status": 401, "message": "로그인이 필요합니다.", "data": null }
		""";

	static final String MEMBER_NOT_FOUND = """
		{ "status": 404, "message": "존재하지 않는 회원입니다.", "data": null }
		""";

	private MemberApiExamples() {
	}
}
