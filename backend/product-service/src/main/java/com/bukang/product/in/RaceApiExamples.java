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

	static final String UPDATE_SUCCESS = """
		{
			"status": 200,
			"message": "대회를 수정했습니다.",
			"data": {
				"raceId": 1,
				"saleStatus": "DRAFT",
				"saleOpenAt": "2026-10-13T10:00:00",
				"courseIds": [1, 2]
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

	static final String COURSE_NOT_IN_RACE = """
		{ "status": 400, "message": "이 대회의 종목이 아니거나 같은 종목을 두 번 보냈습니다. (courseId: 5)", "data": null }
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

	static final String NOT_OWNER = """
		{ "status": 403, "message": "본인이 등록한 대회만 처리할 수 있습니다.", "data": null }
		""";

	static final String RACE_NOT_FOUND = """
		{ "status": 404, "message": "존재하지 않는 대회입니다.", "data": null }
		""";

	static final String PRICE_RAISED = """
		{ "status": 409, "message": "판매가 시작된 종목은 참가비를 올릴 수 없습니다. (종목: 10K)", "data": null }
		""";

	static final String REFUND_DEADLINE_ADVANCED = """
		{ "status": 409, "message": "판매가 시작된 대회는 환불 기준일을 앞당길 수 없습니다.", "data": null }
		""";

	static final String QUOTA_BELOW_TAKEN = """
		{ "status": 409, "message": "정원을 확보·판매된 자리 수보다 줄일 수 없습니다. (종목: 10K)", "data": null }
		""";

	static final String COURSE_HAS_PARTICIPANTS = """
		{ "status": 409, "message": "확보·판매된 자리가 있는 종목은 뺄 수 없습니다. (종목: 10K)", "data": null }
		""";

	private RaceApiExamples() {
	}
}
