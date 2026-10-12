package com.bukang.order.in;

/**
 * InternalV1OrderController의 Swagger 응답 예시 (실제 응답 메시지와 같게 유지한다)
 */
final class OrderInternalApiExamples {
	static final String SOLD_ENTRIES_SUCCESS = """
		{
			"status": 200,
			"message": "판매 내역을 조회했습니다.",
			"data": [
				{ "orderItemId": 1, "sellerId": 3, "raceId": 5, "price": 60000 },
				{ "orderItemId": 4, "sellerId": 3, "raceId": 5, "price": 60000 }
			]
		}
		""";

	static final String SOLD_ENTRIES_EMPTY = """
		{ "status": 200, "message": "판매 내역을 조회했습니다.", "data": [] }
		""";

	static final String MISSING_RACE_DATE = """
		{ "status": 400, "message": "필수 요청 값(raceDate)이 없습니다.", "data": null }
		""";

	static final String INVALID_VALUE_TYPE = """
		{ "status": 400, "message": "요청 값의 형식이 올바르지 않습니다.", "data": null }
		""";

	private OrderInternalApiExamples() {
	}
}
