package com.bukang.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 참가 신청 구분. 세미(1차)는 개인 신청(INDIVIDUAL)만 만든다. 크루원 신청(CREW)은 최종 프로젝트
 */
@Getter
@RequiredArgsConstructor
public enum ParticipationType {
	INDIVIDUAL("개인"),
	CREW("크루원");

	private final String label;
}
