package com.bukang.payout.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 정산 내역 상태
 * 세미의 PAID는 실제 계좌 지급 없이 상태만 바꾸는 모의 지급이다 (docs/policy/payout.md 1장 지급 6번)
 * CANCELED는 대회가 취소돼 지급하지 않고 닫은 정산 내역이다 (PRO-34, payout.md 4-3 [제안]). 다시 열지 않는다
 * PAYING은 캐시 서비스에 정산금 이동을 요청한 뒤 결과를 확정하기 전이다 (PRO-252)
 * 요청 전에 커밋해 두므로, 응답을 못 받아 돈이 옮겨졌는지 모를 때도 보류 · 상계 · 취소가 이 정산 내역을 바꾸지 못한다
 */
@Getter
@RequiredArgsConstructor
public enum PayoutStatus {
	CALCULATED("정산 예정"),
	HELD("지급보류"),
	PAYING("지급 중"),
	PAID("지급 완료"),
	CANCELED("취소");

	private final String korean;
}
