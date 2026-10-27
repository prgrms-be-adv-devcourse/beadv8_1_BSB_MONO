package com.bukang.product.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 같은 서비스 안에서 처리하는 동기화 이벤트(RaceDocumentSyncEvent)의 상태.
 * Kafka 아웃박스(OutboxStatus)는 보냈는지만 알 수 있지만, 이 이벤트는 서비스가 직접 처리하므로 처리했는지까지 기록한다.
 * 그래서 아웃박스(보내는 쪽)와 인박스(받는 쪽)로 나누지 않고 한 테이블에서 미처리 → 처리 완료 또는 처리 실패로 관리한다.
 * 실패는 재시도 횟수를 다 써서 더는 처리하지 않는 상태다. 원인을 고친 뒤 PENDING으로 되돌리면 다시 처리한다.
 */
@Getter
@RequiredArgsConstructor
public enum SyncStatus {
	PENDING("미처리"),
	COMPLETE("처리 완료"),
	FAILED("처리 실패");

	// 화면과 메시지에 쓰는 이름
	private final String label;
}
