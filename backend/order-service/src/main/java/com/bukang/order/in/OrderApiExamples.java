package com.bukang.order.in;

/**
 * ApiV1OrderController의 Swagger 응답 예시 (실제 응답 메시지와 같게 유지한다)
 */
final class OrderApiExamples {
	static final String CREATE_SUCCESS = """
		{
			"status": 201,
			"message": "자리를 확보했습니다. 결제 기한 안에 결제해주세요.",
			"data": {
				"id": 1,
				"createDate": "2026-10-12T10:00:00",
				"orderNo": "OD-20261012-0001",
				"buyerId": 1,
				"raceId": 1,
				"status": "CREATED",
				"totalAmount": 60000,
				"paymentDueAt": "2026-10-12T10:10:00",
				"items": [
					{
						"id": 1,
						"participationId": 1,
						"raceCourseId": 1,
						"sellerId": 1,
						"itemName": "2026 서울 하프 마라톤 10K",
						"unitPrice": 60000,
						"quantity": 1,
						"amount": 60000,
						"status": "ORDERED"
					}
				]
			}
		}
		""";

	static final String CREATE_EXISTING = """
		{
			"status": 200,
			"message": "이미 처리한 주문 요청입니다.",
			"data": {
				"id": 1,
				"createDate": "2026-10-12T10:00:00",
				"orderNo": "OD-20261012-0001",
				"buyerId": 1,
				"raceId": 1,
				"status": "CREATED",
				"totalAmount": 60000,
				"paymentDueAt": "2026-10-12T10:10:00",
				"items": [
					{
						"id": 1,
						"participationId": 1,
						"raceCourseId": 1,
						"sellerId": 1,
						"itemName": "2026 서울 하프 마라톤 10K",
						"unitPrice": 60000,
						"quantity": 1,
						"amount": 60000,
						"status": "ORDERED"
					}
				]
			}
		}
		""";

	static final String INVALID_INPUT = """
		{ "status": 400, "message": "결제할 참가 신청을 선택해주세요.", "data": null }
		""";

	static final String INVALID_BODY = """
		{ "status": 400, "message": "요청 본문 형식이 올바르지 않습니다.", "data": null }
		""";

	static final String MISSING_IDEMPOTENCY_KEY = """
		{ "status": 400, "message": "필수 요청 헤더(Idempotency-Key)가 없습니다.", "data": null }
		""";

	static final String INVALID_IDEMPOTENCY_KEY = """
		{ "status": 400, "message": "Idempotency-Key는 1~64자로 보내주세요.", "data": null }
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
		{ "status": 403, "message": "본인의 참가 신청만 결제할 수 있습니다.", "data": null }
		""";

	static final String PARTICIPATION_NOT_FOUND = """
		{ "status": 404, "message": "존재하지 않는 참가 신청입니다.", "data": null }
		""";

	static final String COURSE_NOT_FOUND = """
		{ "status": 404, "message": "존재하지 않는 종목입니다.", "data": null }
		""";

	static final String SOLD_OUT = """
		{ "status": 409, "message": "남은 자리가 없습니다. 참가 신청서를 다시 작성해 신청할 수 있습니다.", "data": null }
		""";

	static final String NOT_PAYABLE = """
		{ "status": 409, "message": "결제를 시작할 수 없는 신청입니다(현재 상태: 결제 대기).", "data": null }
		""";

	static final String NOT_ACCEPTING = """
		{ "status": 409, "message": "접수 기간이 아닙니다.", "data": null }
		""";

	static final String CONFLICT = """
		{ "status": 409, "message": "같은 주문 요청을 처리하고 있습니다. 잠시 뒤 주문 내역을 확인해주세요.", "data": null }
		""";

	static final String KEY_REUSED = """
		{ "status": 409, "message": "이미 다른 주문에 쓴 Idempotency-Key입니다. 새 키로 다시 요청해주세요.", "data": null }
		""";

	private OrderApiExamples() {
	}
}
