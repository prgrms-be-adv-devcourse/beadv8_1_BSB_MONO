package com.bukang.product.app;

import com.bukang.common.shared.race.event.RaceCancelEvent;

/**
 * 대회 취소 유스케이스의 결과. ProductFacade가 커밋한 뒤 검색 색인과 취소 메시지 발행에 쓴다.
 */
public record RaceCancelResult(
	// API 응답과 다시 색인할 종목 문서 (판매 상태가 대회 취소로 바뀌어 공개 목록에서 빠진다)
	RaceSaveResult saved,
	// 주문 서비스에 보낼 취소 메시지
	RaceCancelEvent cancelEvent,
	// 해제한 결제 대기 자리 수
	int releasedHolds
) {
}
