package com.bukang.common.shared.kafka;

/**
 * Kafka 토픽 이름 (생산자와 소비자가 같은 상수를 쓴다. 형식은 .claude/rules/kafka-topic-convention.md)
 * 토픽 이름에는 보내고 받는 서비스 이름을 넣지 않는다. 같은 토픽을 여러 서비스가 각자의 groupId로 받는다.
 */
public final class KafkaTopics {
	public static final String MEMBER_JOINED = "queuing.member.joined";
	public static final String SELLER_CHANGED = "queuing.seller.changed";

	private KafkaTopics() {
	}
}
