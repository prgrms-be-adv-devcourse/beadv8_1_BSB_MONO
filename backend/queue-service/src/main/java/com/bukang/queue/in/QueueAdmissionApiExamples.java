package com.bukang.queue.in;

/**
 * InternalV1QueueController의 Swagger 응답 예시 (실제 응답 메시지와 같게 유지한다)
 * 요청 값 오류·회원 ID 헤더 누락은 순번 조회와 같은 응답이라 QueueApiExamples의 상수를 함께 쓴다
 */
final class QueueAdmissionApiExamples {
	static final String ADMITTED = """
		{
			"status": 200,
			"message": "대기열에 입장 중입니다.",
			"data": {
				"admitted": true,
				"admissionEndsAt": "2026-10-16T14:10:00",
				"remainingMillis": 540000
			}
		}
		""";

	static final String NOT_ADMITTED = """
		{
			"status": 200,
			"message": "대기열에 입장 중이 아닙니다.",
			"data": {
				"admitted": false,
				"admissionEndsAt": null,
				"remainingMillis": 0
			}
		}
		""";

	private QueueAdmissionApiExamples() {
	}
}
