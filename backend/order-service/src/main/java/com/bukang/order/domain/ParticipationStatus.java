package com.bukang.order.domain;

import java.util.EnumSet;
import java.util.Set;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 참가 신청 상태 (ERD 추천안 v4 8쪽)
 * PENDING → APPLIED(자리 확보) → CONFIRMED(결제 완료). 확보 실패는 FAILED, 미결제 해제는 RELEASED, 취소는 CANCELED
 */
@Getter
@RequiredArgsConstructor
public enum ParticipationStatus {
	PENDING("확보 전"),
	APPLIED("결제 대기"),
	CONFIRMED("참가 확정"),
	RELEASED("미결제 해제"),
	FAILED("확보 실패"),
	CANCELED("취소");

	// 살아 있는 신청: 한 사람 한 대회 1건(active_flag)에 걸리고, 접수 마감 전까지 참가자 정보를 고칠 수 있다
	private static final Set<ParticipationStatus> ACTIVE = EnumSet.of(PENDING, APPLIED, CONFIRMED);

	// 화면과 메시지에 쓰는 이름
	private final String label;

	public boolean isActive() {
		return ACTIVE.contains(this);
	}
}
