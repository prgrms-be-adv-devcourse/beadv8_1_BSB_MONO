package com.bukang.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 참가자 성별 (대회 기록·시상 구분에 쓰인다)
 */
@Getter
@RequiredArgsConstructor
public enum Gender {
	MALE("남성"),
	FEMALE("여성");

	private final String label;
}
