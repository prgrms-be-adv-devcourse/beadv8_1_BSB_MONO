package com.bukang.payout.in;

/**
 * InternalV1PayoutController의 Swagger 응답 예시 (실제 응답 메시지와 같게 유지한다)
 */
final class PayoutInternalApiExamples {
	static final String REFUNDABLE = """
		{
			"status": 200,
			"message": "환불 가능 여부를 조회했습니다.",
			"data": {
				"raceDate": "2026-09-25",
				"payoutTargetDate": "2026-09-26",
				"refundable": true
			}
		}
		""";

	static final String NOT_REFUNDABLE = """
		{
			"status": 200,
			"message": "환불 가능 여부를 조회했습니다.",
			"data": {
				"raceDate": "2026-09-25",
				"payoutTargetDate": "2026-09-26",
				"refundable": false
			}
		}
		""";

	static final String MISSING_PARAMETER = """
		{ "status": 400, "message": "필수 요청 값이 없습니다.", "data": null }
		""";

	static final String INVALID_VALUE_TYPE = """
		{ "status": 400, "message": "요청 값의 형식이 올바르지 않습니다.", "data": null }
		""";

	private PayoutInternalApiExamples() {
	}
}
