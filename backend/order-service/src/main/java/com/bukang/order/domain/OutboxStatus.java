package com.bukang.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Kafka 아웃박스 행(OrderOutboxKafkaEvent)의 상태: 미처리 → 발행 완료(SENT) 또는 발행 실패(SEND_FAILED).
 * 아웃박스는 메시지를 보냈는지만 기록한다. 받는 쪽이 처리했는지는 이 서비스가 알 수 없다 (처리 여부는 받는 쪽 몫이다).
 * 발행 실패는 재시도 횟수를 다 써서 더는 보내지 않는 상태다. 원인을 고친 뒤 PENDING으로 되돌리면 다시 보낸다.
 * 상품 서비스의 같은 이름 enum과 값을 맞췄다 (서비스끼리 의존하지 않아 따로 둔다)
 */
@Getter
@RequiredArgsConstructor
public enum OutboxStatus {
	PENDING("미처리"),
	// 메시지를 브로커에 보냈다
	SENT("발행 완료"),
	// 메시지를 브로커에 끝내 보내지 못했다
	SEND_FAILED("발행 실패");

	// 화면과 메시지에 쓰는 이름
	private final String label;
}
