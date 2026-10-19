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

	static final String CHARGE_CONFIRM_SUCCESS = """
		{
			"status": 200,
			"message": "충전했습니다.",
			"data": {
				"outcome": "COMPLETED",
				"balance": 60000
			}
		}
		""";

	static final String CHARGE_CONFIRM_FAILED = """
		{
			"status": 200,
			"message": "충전에 실패했습니다.",
			"data": {
				"outcome": "FAILED",
				"balance": 50000
			}
		}
		""";

	static final String CHARGE_AMOUNT_TOO_SMALL = """
		{ "status": 400, "message": "최소 충전 금액은 10,000원입니다.", "data": null }
		""";

	static final String CHARGE_CONFIRM_INVALID_INPUT = """
		{ "status": 400, "code": "INVALID_REQUEST", "message": "paymentKey를 작성해주세요.", "data": null }
		""";

	static final String INVALID_BODY = """
		{ "status": 400, "code": "INVALID_REQUEST", "message": "요청 본문 형식이 올바르지 않습니다.", "data": null }
		""";

	static final String INVALID_VALUE_TYPE = """
		{ "status": 400, "code": "INVALID_REQUEST", "message": "요청 값의 형식이 올바르지 않습니다.", "data": null }
		""";

	static final String INVALID_MEMBER_ID = """
		{ "status": 400, "code": "INVALID_REQUEST", "message": "회원 ID가 올바르지 않습니다.", "data": null }
		""";

	static final String LOGIN_REQUIRED = """
		{ "status": 401, "message": "로그인이 필요합니다.", "data": null }
		""";

	static final String WITHDRAWAL_SUCCESS = """
		{
			"status": 200,
			"message": "환급 신청이 완료되었습니다.",
			"data": {
				"withdrawnAmount": 50000,
				"balance": 0
			}
		}
		""";

	static final String WALLET_BALANCE_EMPTY = """
		{ "status": 400, "message": "환급할 잔액이 없습니다.", "data": null }
		""";

	static final String WITHDRAWAL_INVALID_INPUT = """
		{ "status": 400, "code": "INVALID_REQUEST", "message": "은행 코드를 작성해주세요.", "data": null }
		""";

	private CashApiExamples() {
	}
}
