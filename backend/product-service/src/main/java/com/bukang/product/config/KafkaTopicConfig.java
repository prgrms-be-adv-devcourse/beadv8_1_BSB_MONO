package com.bukang.product.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import com.bukang.common.shared.kafka.KafkaTopics;

/**
 * 상품 서비스가 받는 토픽의 파티션 수를 정한다 (기본 자동 생성은 브로커 기본값 1개)
 * 앱이 뜰 때 KafkaAdmin이 토픽을 만들고, 이미 있는 토픽은 파티션을 늘린다 (줄이지는 못한다).
 * 같은 판매자의 메시지 순서는 파티션 안에서만 지켜지므로, 보내는 쪽은 ID를 key로 보내야 한다.
 * 회원 토픽(queuing.member.joined, queuing.member.modified)은 회원 서비스가 보내고 캐시 서비스도 받는 공용 토픽이라 여기서 정하지 않는다.
 */
@Configuration
public class KafkaTopicConfig {
	// ProductKafkaConsumer의 PARTITIONS(재시도·DLT 토픽 파티션 수)와 맞춘다
	// 동시 처리 쓰레드 수(ProductKafkaConsumer.CONCURRENCY)는 이 수까지만 효과가 있다
	// 지금은 메시지가 적어 1개로 둔다. 늘리면 앱이 뜰 때 기존 토픽의 파티션이 늘어난다
	private static final int PARTITIONS = 1;

	// 복제 수는 정하지 않아 브로커 기본값을 따른다 (로컬 Redpanda는 1)
	@Bean
	public NewTopic sellerChangedTopic() {
		return TopicBuilder.name(KafkaTopics.SELLER_CHANGED)
			.partitions(PARTITIONS)
			.build();
	}

	@Bean
	public NewTopic productHoldCancelTopic() {
		return TopicBuilder.name(KafkaTopics.PRODUCT_HOLD_CANCEL)
			.partitions(PARTITIONS)
			.build();
	}

	@Bean
	public NewTopic productHoldRestoreTopic() {
		return TopicBuilder.name(KafkaTopics.PRODUCT_HOLD_RESTORE)
			.partitions(PARTITIONS)
			.build();
	}
}
