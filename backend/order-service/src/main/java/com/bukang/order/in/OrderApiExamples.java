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
				"canceledAmount": 0,
				"paymentDueAt": "2026-10-12T10:10:00",
				"paidAt": null,
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
				"canceledAmount": 0,
				"paymentDueAt": "2026-10-12T10:10:00",
				"paidAt": null,
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

	static final String EXTEND_SUCCESS = """
		{
			"status": 200,
			"message": "결제 기한을 2분 늘렸습니다.",
			"data": {
				"id": 1,
				"createDate": "2026-10-12T10:00:00",
				"orderNo": "OD-20261012-0001",
				"buyerId": 1,
				"raceId": 1,
				"status": "CREATED",
				"totalAmount": 60000,
				"canceledAmount": 0,
				"paymentDueAt": "2026-10-12T10:12:00",
				"paidAt": null,
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

	static final String ORDER_NOT_OWNER = """
		{ "status": 403, "message": "본인의 주문만 결제 기한을 늘릴 수 있습니다.", "data": null }
		""";

	static final String ORDER_NOT_FOUND = """
		{ "status": 404, "message": "존재하지 않는 주문입니다.", "data": null }
		""";

	static final String ALREADY_EXTENDED = """
		{ "status": 409, "message": "결제 기한은 한 번만 늘릴 수 있습니다.", "data": null }
		""";

	static final String DEADLINE_PASSED = """
		{ "status": 409, "message": "결제 기한이 지났습니다.", "data": null }
		""";

	static final String NOT_WAITING_PAYMENT = """
		{ "status": 409, "message": "결제 대기 중인 주문만 결제 기한을 늘릴 수 있습니다(현재 상태: 기한 만료).", "data": null }
		""";

	static final String FIND_SUCCESS = """
		{
			"status": 200,
			"message": "주문을 조회했습니다.",
			"data": {
				"id": 1,
				"createDate": "2026-10-12T10:00:00",
				"orderNo": "OD-20261012-0001",
				"buyerId": 1,
				"raceId": 1,
				"status": "PAID",
				"totalAmount": 60000,
				"canceledAmount": 0,
				"paymentDueAt": "2026-10-12T10:10:00",
				"paidAt": "2026-10-12T10:04:12",
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
						"status": "PAID"
					}
				]
			}
		}
		""";

	static final String LIST_SUCCESS = """
		{
			"status": 200,
			"message": "주문 내역을 조회했습니다.",
			"data": {
				"orders": [
					{
						"id": 2,
						"createDate": "2026-10-12T10:00:00",
						"orderNo": "OD-20261012-0002",
						"status": "CREATED",
						"totalAmount": 60000,
						"paymentDueAt": "2026-10-12T10:10:00",
						"itemName": "2026 서울 하프 마라톤 10K",
						"itemCount": 1
					},
					{
						"id": 1,
						"createDate": "2026-10-10T09:00:00",
						"orderNo": "OD-20261010-0001",
						"status": "EXPIRED",
						"totalAmount": 45000,
						"paymentDueAt": "2026-10-10T09:10:00",
						"itemName": "2026 부산 바다 마라톤 5K",
						"itemCount": 1
					}
				],
				"page": 0,
				"size": 20,
				"totalElements": 2,
				"totalPages": 1
			}
		}
		""";

	static final String INVALID_PERIOD = """
		{ "status": 400, "message": "조회 시작일이 종료일보다 늦습니다.", "data": null }
		""";

	static final String INVALID_PAGE = """
		{ "status": 400, "message": "한 번에 100개까지 조회할 수 있습니다.", "data": null }
		""";

	static final String VIEW_NOT_OWNER = """
		{ "status": 403, "message": "본인의 주문만 볼 수 있습니다.", "data": null }
		""";

	static final String PAY_SUCCESS = """
		{
			"status": 200,
			"message": "결제했습니다. 참가가 확정되었습니다.",
			"data": {
				"id": 1,
				"createDate": "2026-10-12T10:00:00",
				"orderNo": "OD-20261012-0001",
				"buyerId": 1,
				"raceId": 1,
				"status": "PAID",
				"totalAmount": 60000,
				"canceledAmount": 0,
				"paymentDueAt": "2026-10-12T10:10:00",
				"paidAt": "2026-10-12T10:04:12",
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
						"status": "PAID"
					}
				]
			}
		}
		""";

	static final String PAY_ALREADY = """
		{
			"status": 200,
			"message": "이미 결제한 주문입니다.",
			"data": { "id": 1, "orderNo": "OD-20261012-0001", "status": "PAID", "paidAt": "2026-10-12T10:04:12" }
		}
		""";

	static final String PAY_NOT_OWNER = """
		{ "status": 403, "message": "본인의 주문만 결제할 수 있습니다.", "data": null }
		""";

	static final String INSUFFICIENT_BALANCE = """
		{ "status": 409, "message": "예치금이 12,000원 부족합니다. 충전한 뒤 결제 기한 안에 다시 결제해주세요.", "data": null }
		""";

	static final String PAY_DEADLINE_PASSED = """
		{ "status": 409, "message": "결제 기한이 지났습니다.", "data": null }
		""";

	static final String SELL_EXPIRED = """
		{ "status": 409, "message": "결제 기한이 지나 결제하지 못했습니다. 빠져나간 예치금은 돌려드렸습니다.", "data": null }
		""";

	static final String NOT_PAYABLE_ORDER = """
		{ "status": 409, "message": "결제할 수 없는 주문입니다(현재 상태: 기한 만료).", "data": null }
		""";

	static final String CANCEL_SUCCESS = """
		{
			"status": 200,
			"message": "취소했습니다. 예치금 60,000원을 돌려드렸습니다.",
			"data": {
				"id": 1,
				"createDate": "2026-10-12T10:00:00",
				"orderNo": "OD-20261012-0001",
				"buyerId": 1,
				"raceId": 1,
				"status": "CANCELED",
				"totalAmount": 60000,
				"canceledAmount": 60000,
				"paymentDueAt": "2026-10-12T10:10:00",
				"paidAt": "2026-10-12T10:04:12",
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
						"status": "CANCELED"
					}
				]
			}
		}
		""";

	static final String CANCEL_NOT_OWNER = """
		{ "status": 403, "message": "본인의 주문만 취소할 수 있습니다.", "data": null }
		""";

	static final String ORDER_ITEM_NOT_FOUND = """
		{ "status": 404, "message": "주문에 없는 상품입니다.", "data": null }
		""";

	static final String ALREADY_CANCELED = """
		{ "status": 409, "message": "이미 취소한 상품입니다.", "data": null }
		""";

	static final String NOT_PAID_ITEM = """
		{ "status": 409, "message": "결제한 상품만 취소할 수 있습니다(현재 상태: 결제 대기).", "data": null }
		""";

	static final String REFUND_PERIOD_PASSED = """
		{ "status": 409, "message": "환불 기한(2026-07-15)이 지나 취소할 수 없습니다.", "data": null }
		""";

	static final String SCHEDULE_CHANGE_REFUND_PASSED = """
		{
			"status": 409,
			"message": "환불 기한(2026-07-15)과 일정 변경 뒤 전액 환불 기간(2026-07-25까지)이 지나 취소할 수 없습니다.",
			"data": null
		}
		""";

	static final String PAYOUT_UNAVAILABLE = """
		{ "status": 503, "message": "정산 서비스에 연결하지 못했습니다. 잠시 뒤 다시 시도해주세요.", "data": null }
		""";

	static final String PAYOUT_STARTED = """
		{ "status": 409, "message": "정산이 시작된 참가권이라 취소할 수 없습니다.", "data": null }
		""";

	private OrderApiExamples() {
	}
}
