package com.bukang.product.app;

import com.bukang.product.dto.RaceSaveDto;

/**
 * 대회 등록 결과. 같은 판매자가 같은 Idempotency-Key로 다시 보내면 새로 만들지 않고 처음 등록한 대회를 돌려준다
 * 컨트롤러가 결과를 보고 201(새로 등록)과 200(이미 등록한 대회)을 고른다 (order-service OrderCreateResult와 같은 방식)
 */
public record RaceRegisterResult(Outcome outcome, RaceSaveDto race) {
	static RaceRegisterResult created(RaceSaveDto race) {
		return new RaceRegisterResult(Outcome.CREATED, race);
	}

	static RaceRegisterResult existing(RaceSaveDto race) {
		return new RaceRegisterResult(Outcome.EXISTING, race);
	}

	public enum Outcome {
		// 이번 요청으로 새로 등록했다
		CREATED,
		// 같은 키로 이미 등록한 대회다. 검색 색인과 메시지는 처음 등록 때 보냈으므로 다시 보내지 않는다
		EXISTING
	}
}
