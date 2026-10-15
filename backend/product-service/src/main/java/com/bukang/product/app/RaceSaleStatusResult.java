package com.bukang.product.app;

import java.util.List;

import com.bukang.common.shared.race.event.RaceChangedEvent;
import com.bukang.product.domain.document.RaceCourseDocument;

/**
 * 판매 상태 배치의 결과. ProductFacade가 커밋한 뒤 검색 색인과 대회 변경 메시지 발행에 쓴다.
 */
public record RaceSaleStatusResult(
	// 다시 색인할 종목 문서 (상태가 바뀐 대회의 모든 종목)
	List<RaceCourseDocument> documents,
	// 대기열에 보낼 대회 변경 메시지 (상태가 바뀐 대회마다 1건)
	List<RaceChangedEvent> raceChanges
) {
}
