package com.bukang.product.in;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.bukang.common.shared.kafka.KafkaTopics;

import lombok.extern.slf4j.Slf4j;

/**
 * 재시도를 모두 실패해 DLT(<토픽>.product.dlt)로 간 메시지를 받아 에러 로그로 남긴다
 * 다시 처리하지 않는다. 원인을 고친 뒤 필요하면 Kafka Console(http://localhost:8091)에서 메시지를 확인해 다시 보낸다.
 */
@Slf4j
@Component
public class ProductKafkaDltConsumer {
	// 본 리스너(product)와 오프셋을 따로 관리한다
	private static final String GROUP_ID = "product-dlt";
	private static final String MEMBER_JOINED_DLT = KafkaTopics.MEMBER_MEMBER_JOINED
		+ ProductKafkaConsumer.DLT_TOPIC_SUFFIX;
	private static final String MEMBER_MODIFIED_DLT = KafkaTopics.MEMBER_MODIFIED
		+ ProductKafkaConsumer.DLT_TOPIC_SUFFIX;
	private static final String SELLER_CHANGED_DLT = KafkaTopics.SELLER_CHANGED
		+ ProductKafkaConsumer.DLT_TOPIC_SUFFIX;
	private static final String HOLD_CANCEL_DLT = KafkaTopics.PRODUCT_HOLD_CANCEL
		+ ProductKafkaConsumer.DLT_TOPIC_SUFFIX;

	@KafkaListener(topics = MEMBER_JOINED_DLT, groupId = GROUP_ID)
	public void handleMemberJoinedDlt(String payload) {
		log.error("처리하지 못한 회원가입 메시지: {}", payload);
	}

	@KafkaListener(topics = MEMBER_MODIFIED_DLT, groupId = GROUP_ID)
	public void handleMemberModifiedDlt(String payload) {
		log.error("처리하지 못한 회원 변경 메시지: {}", payload);
	}

	@KafkaListener(topics = SELLER_CHANGED_DLT, groupId = GROUP_ID)
	public void handleSellerChangedDlt(String payload) {
		log.error("처리하지 못한 판매자 메시지: {}", payload);
	}

	// 해제하지 못한 자리는 결제 대기로 남아 남은 자리가 그만큼 적게 보인다. 원인을 고친 뒤 메시지를 다시 보내야 한다
	@KafkaListener(topics = HOLD_CANCEL_DLT, groupId = GROUP_ID)
	public void handleHoldCancelDlt(String payload) {
		log.error("처리하지 못한 임시 확보 해제 메시지: {}", payload);
	}
}
