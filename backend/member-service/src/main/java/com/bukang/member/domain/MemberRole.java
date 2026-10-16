package com.bukang.member.domain;

/**
 * 회원 역할. 관리자 API는 X-Member-Id 회원의 역할이 ADMIN인지 DB에서 확인한다 (BE-020)
 */
public enum MemberRole {
	USER,
	ADMIN
}
