package com.bukang.order.in;

/**
 * ApiV1CartController의 Swagger 응답 예시 (실제 응답 메시지와 같게 유지한다)
 */
final class CartApiExamples {
	static final String FIND_SUCCESS = """
		{
			"status": 200,
			"message": "장바구니를 조회했습니다.",
			"data": {
				"memberId": 1,
				"items": [
					{ "id": 2, "createDate": "2026-10-12T10:20:00", "raceCourseId": 3 },
					{ "id": 1, "createDate": "2026-10-12T10:15:30", "raceCourseId": 1 }
				]
			}
		}
		""";

	static final String FIND_EMPTY = """
		{ "status": 200, "message": "장바구니를 조회했습니다.", "data": { "memberId": 1, "items": [] } }
		""";

	static final String ADD_SUCCESS = """
		{
			"status": 201,
			"message": "장바구니에 담았습니다.",
			"data": { "id": 1, "createDate": "2026-10-12T10:15:30", "raceCourseId": 1 }
		}
		""";

	static final String ADD_ALREADY = """
		{
			"status": 200,
			"message": "이미 장바구니에 담긴 참가권입니다.",
			"data": { "id": 1, "createDate": "2026-10-12T10:15:30", "raceCourseId": 1 }
		}
		""";

	static final String REMOVE_SUCCESS = """
		{
			"status": 200,
			"message": "장바구니에서 뺐습니다.",
			"data": {
				"memberId": 1,
				"items": [
					{ "id": 2, "createDate": "2026-10-12T10:20:00", "raceCourseId": 3 }
				]
			}
		}
		""";

	static final String INVALID_INPUT = """
		{ "status": 400, "message": "담을 종목을 선택해주세요.", "data": null }
		""";

	static final String INVALID_BODY = """
		{ "status": 400, "message": "요청 본문 형식이 올바르지 않습니다.", "data": null }
		""";

	static final String INVALID_VALUE_TYPE = """
		{ "status": 400, "message": "요청 값의 형식이 올바르지 않습니다.", "data": null }
		""";

	static final String INVALID_MEMBER_ID = """
		{ "status": 400, "message": "회원 ID가 올바르지 않습니다.", "data": null }
		""";

	static final String LOGIN_REQUIRED = """
		{ "status": 401, "message": "로그인이 필요합니다.", "data": null }
		""";

	static final String NOT_OWNER = """
		{ "status": 403, "message": "본인의 장바구니 항목만 뺄 수 있습니다.", "data": null }
		""";

	static final String COURSE_NOT_FOUND = """
		{ "status": 404, "message": "존재하지 않는 종목입니다.", "data": null }
		""";

	static final String ITEM_NOT_FOUND = """
		{ "status": 404, "message": "장바구니에 없는 항목입니다.", "data": null }
		""";

	static final String APPLICATION_CLOSED = """
		{ "status": 409, "message": "접수가 마감된 종목은 담을 수 없습니다.", "data": null }
		""";

	static final String CONFLICT = """
		{ "status": 409, "message": "장바구니에 담는 요청이 겹쳤습니다. 다시 시도해주세요.", "data": null }
		""";

	private CartApiExamples() {
	}
}
