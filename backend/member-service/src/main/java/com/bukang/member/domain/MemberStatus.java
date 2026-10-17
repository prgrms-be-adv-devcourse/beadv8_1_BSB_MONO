package com.bukang.member.domain;

/**
 * 회원 상태 (docs member-table 5장). 판단은 이 값으로, 언제 바뀌었는지는 날짜 칸(withdrawn_at)으로 남긴다
 * 지금은 ACTIVE → WITHDRAWN으로 바로 간다 (BE-022). WITHDRAWING은 예치금 환급 대기를 붙일 때 쓴다
 * WITHDRAWN → ACTIVE는 없다 (개인정보를 이미 지웠으므로)
 */
public enum MemberStatus {
	ACTIVE,
	WITHDRAWING,
	WITHDRAWN
}
