package com.bukang.product.in;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import com.bukang.common.shared.kafka.KafkaTopics;

import lombok.extern.slf4j.Slf4j;

/**
 * 재시도를 모두 실패해 DLT(<토픽>.dlt)로 간 메시지를 받아 에러 로그로 남긴다
 * 다시 처리하지 않는다. 원인을 고친 뒤 필요하면 Kafka Console(http://localhost:8091)에서 메시지를 확인해 다시 보낸다.
 */
@Slf4j
@Component
public class ProductKafkaDltConsumer {
	// 본 리스너(product)와 오프셋을 따로 관리한다
	private static final String GROUP_ID = "product-dlt";
	private static final String MEMBER_JOINED_DLT = KafkaTopics.PRODUCT_MEMBER_JOINED
		+ ProductKafkaConsumer.DLT_TOPIC_SUFFIX;
	private static final String SELLER_CHANGED_DLT = KafkaTopics.PRODUCT_SELLER_CHANGED
		+ ProductKafkaConsumer.DLT_TOPIC_SUFFIX;

	@KafkaListener(topics = MEMBER_JOINED_DLT, groupId = GROUP_ID)
	public void handleMemberJoinedDlt(ConsumerRecord<String, String> record) {
		logFailure(record);
	}

	@KafkaListener(topics = SELLER_CHANGED_DLT, groupId = GROUP_ID)
	public void handleSellerChangedDlt(ConsumerRecord<String, String> record) {
		logFailure(record);
	}

	private void logFailure(ConsumerRecord<String, String> record) {
		log.error("처리하지 못한 메시지가 DLT에 들어왔습니다. topic={}, partition={}, offset={}, key={}, 원인={}, payload={}",
			record.topic(), record.partition(), record.offset(), record.key(),
			headerValue(record, KafkaHeaders.EXCEPTION_MESSAGE), record.value());
	}

	// 재시도·DLT로 보낼 때 Spring Kafka가 붙인 헤더 (값은 바이트 배열). 예외 메시지는 kafka_exception-message에 있다
	private String headerValue(ConsumerRecord<String, String> record, String name) {
		Header header = record.headers().lastHeader(name);
		return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
	}
}
