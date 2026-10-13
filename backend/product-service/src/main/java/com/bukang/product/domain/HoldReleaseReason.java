package com.bukang.product.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 결제 대기 자리를 해제한 사유 (ERD v5 participation_hold.release_reason에 대회 취소를 더했다)
 */
@Getter
@RequiredArgsConstructor
public enum HoldReleaseReason {
	// 대기열을 통과한 뒤의 활성 시간이 끝나 주문 서비스가 해제를 알렸다
	EXPIRED("기한 초과"),
	// 크루 예매용 사유 (세미는 크루 예매를 하지 않아 쓰지 않는다)
	CREW_CANCELED("크루장 취소"),
	LEFT_CREW("크루 탈퇴"),
	// 판매자가 대회를 취소했다
	RACE_CANCELED("대회 취소");

	// 화면과 메시지에 쓰는 이름
	private final String label;
}
