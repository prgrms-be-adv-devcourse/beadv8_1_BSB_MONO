package com.bukang.cash.in;

/**
 * ApiV1CashController의 Swagger 응답 예시 (실제 응답 메시지와 같게 유지한다)
 */
final class CashApiExamples {
	static final String GET_BALANCE_SUCCESS = """
		{
			"status": 200,
			"message": "잔액 조회 성공",
			"data": {
				"holderId": 3,
				"balance": 50000
			}
		}
		""";

	static final String WALLET_NOT_FOUND = """
		{ "status": 404, "message": "존재하지 않는 지갑입니다.", "data": null }
		""";

	private CashApiExamples() {
	}
}
