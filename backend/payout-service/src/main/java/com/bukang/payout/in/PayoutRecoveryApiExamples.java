package com.bukang.payout.in;

/**
 * ApiV1PayoutRecoveryController의 Swagger 응답 예시 (실제 응답 메시지와 같게 유지한다)
 */
final class PayoutRecoveryApiExamples {
	static final String COMPLETE_SUCCESS = """
		{
			"status": 200,
			"message": "회수를 완료했습니다.",
			"data": {
				"id": 1,
				"sellerId": 1,
				"raceId": 10,
				"reasonType": "RACE_CANCELED",
				"amount": 40000,
				"status": "COMPLETED",
				"requestedAt": "2026-10-15T14:00:00",
				"completedBy": 1,
				"completedAt": "2026-10-20T10:00:00"
			}
		}
		""";

	static final String RECOVERY_NOT_FOUND = """
		{ "status": 404, "message": "회수를 찾을 수 없습니다.", "data": null }
		""";

	static final String ALREADY_COMPLETED = """
		{ "status": 409, "message": "이미 완료된 회수입니다.", "data": null }
		""";

	private PayoutRecoveryApiExamples() {
	}
}
