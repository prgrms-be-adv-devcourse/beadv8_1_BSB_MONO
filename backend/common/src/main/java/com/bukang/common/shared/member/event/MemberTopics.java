package com.bukang.common.shared.member.event;

/**
 * 회원 이벤트 토픽 이름 (member-service가 발행하고 다른 서비스가 구독한다)
 * 생산자와 소비자가 이 상수를 함께 참조해, 토픽 이름이 어긋나 이벤트를 못 받는 일을 막는다.
 */
public final class MemberTopics {
	public static final String MEMBER_JOINED = "queuing.member.joined";
	public static final String MEMBER_MODIFIED = "queuing.member.modified";

	private MemberTopics() {
	}
}
