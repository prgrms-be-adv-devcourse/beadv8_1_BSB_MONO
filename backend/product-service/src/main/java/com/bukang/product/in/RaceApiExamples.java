package com.bukang.product.in;

/**
 * ApiV1RaceController의 Swagger 응답 예시 (실제 응답 메시지와 같게 유지한다)
 */
final class RaceApiExamples {
	static final String REGISTER_SUCCESS = """
		{
			"status": 201,
			"message": "대회가 작성 중으로 저장되었습니다. 판매 시작을 눌러야 2026-10-13 10:00부터 판매됩니다.",
			"data": {
				"raceId": 1,
				"saleStatus": "DRAFT",
				"saleOpenAt": "2026-10-13T10:00:00",
				"courseIds": [1]
			}
		}
		""";

	static final String REGISTER_INVALID_INPUT = """
		{ "status": 400, "message": "참가비는 0원보다 커야 합니다.", "data": null }
		""";

	static final String INVALID_BODY = """
		{ "status": 400, "message": "요청 본문 형식이 올바르지 않습니다.", "data": null }
		""";

	static final String INVALID_SCHEDULE = """
		{ "status": 400, "message": "크루 예매 마감 일시는 시작 일시보다 늦어야 합니다.", "data": null }
		""";

	static final String THUMBNAIL_REQUIRED = """
		{ "status": 400, "message": "대표 이미지를 1장 연결해주세요.", "data": null }
		""";

	static final String DUPLICATE_FILE = """
		{ "status": 400, "message": "같은 파일을 두 번 연결할 수 없습니다. (fileId: 3, fileType: COURSE)", "data": null }
		""";

	static final String INVALID_VALUE_TYPE = """
		{ "status": 400, "message": "요청 값의 형식이 올바르지 않습니다.", "data": null }
		""";

	static final String LOGIN_REQUIRED = """
		{ "status": 401, "message": "로그인이 필요합니다.", "data": null }
		""";

	static final String SELLER_NOT_APPROVED = """
		{ "status": 403, "message": "승인된 판매자만 대회를 등록·수정할 수 있습니다.", "data": null }
		""";

	private RaceApiExamples() {
	}
}
