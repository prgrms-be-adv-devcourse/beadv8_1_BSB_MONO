package com.bukang.product.app;

import java.util.List;

import com.bukang.common.shared.race.event.RaceChangedEvent;

/**
 * 판매 상태 배치의 결과. ProductFacade가 같은 트랜잭션에서 대회 변경 메시지와 검색 문서 동기화 이벤트를 남기는 데 쓴다.
 */
public record RaceSaleStatusResult(
	// 대기열에 보낼 대회 변경 메시지 (상태가 바뀐 대회마다 1건)
	List<RaceChangedEvent> raceChanges
) {
}
