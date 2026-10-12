package com.bukang.common.shared.kafka;

/**
 * Kafka 토픽 이름 (생산자와 소비자가 같은 상수를 쓴다)
 * 형식은 <받는 서비스>.<도메인>.<이벤트>다. 토픽 이름만 보고 어느 서비스로 가는 메시지인지 알 수 있도록,
 * 같은 이벤트도 받는 서비스마다 토픽을 나눈다 (예: product.member.joined, cash.member.joined).
 */
public final class KafkaTopics {
	public static final String PRODUCT_MEMBER_JOINED = "product.member.joined";
	public static final String PRODUCT_SELLER_CHANGED = "product.seller.changed";

	private KafkaTopics() {
	}
}
