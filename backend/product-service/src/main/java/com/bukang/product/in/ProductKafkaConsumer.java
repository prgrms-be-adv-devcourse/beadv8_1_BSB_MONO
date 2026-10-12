package com.bukang.product.in;

import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;

import com.bukang.common.global.json.JsonConverter;
import com.bukang.common.shared.kafka.KafkaTopics;
import com.bukang.common.shared.member.event.MemberJoinedEvent;
import com.bukang.common.shared.seller.event.SellerChangedEvent;
import com.bukang.product.app.ProductFacade;

import lombok.RequiredArgsConstructor;

/**
 * 회원·판매자 이벤트를 받아 복제본(ProductMember, ProductSeller)에 저장한다
 * 메시지는 JSON 문자열로 받아 JsonConverter로 바꾼다.
 * 처리 중 예외가 나면 재시도 토픽으로 보내 간격을 늘려 가며 다시 시도하고, 그래도 실패하면 <토픽>.dlt로 보낸다.
 */
@Component
@RequiredArgsConstructor
public class ProductKafkaConsumer {
	// application.yaml에 group-id가 없으므로 리스너마다 적는다
	private static final String GROUP_ID = "product";
	// 멀티 쓰레드로 병렬 처리할 파티션 수 (토픽의 파티션이 이 수보다 적으면 남는 쓰레드는 놀게 된다)
	private static final String CONCURRENCY = "3";
	// 처음 시도를 포함한 시도 횟수. 1초 → 2초 → 4초 → 8초 간격으로 다시 시도한다
	private static final String RETRY_ATTEMPTS = "5";
	private static final long RETRY_DELAY_MILLIS = 1000L;
	private static final double RETRY_MULTIPLIER = 2.0;
	// 마지막까지 실패한 메시지를 보낼 토픽 접미사 (예: product.member.joined.dlt, 기본값은 -dlt)
	private static final String DLT_TOPIC_SUFFIX = ".dlt";

	private final ProductFacade productFacade;
	private final JsonConverter jsonConverter;

	@KafkaListener(topics = KafkaTopics.PRODUCT_MEMBER_JOINED, groupId = GROUP_ID, concurrency = CONCURRENCY)
	@RetryableTopic(
		attempts = RETRY_ATTEMPTS,
		backOff = @BackOff(delay = RETRY_DELAY_MILLIS, multiplier = RETRY_MULTIPLIER),
		dltTopicSuffix = DLT_TOPIC_SUFFIX
	)
	public void handleMemberJoined(String payload) {
		productFacade.syncMember(jsonConverter.fromJson(payload, MemberJoinedEvent.class));
	}

	@KafkaListener(topics = KafkaTopics.PRODUCT_SELLER_CHANGED, groupId = GROUP_ID, concurrency = CONCURRENCY)
	@RetryableTopic(
		attempts = RETRY_ATTEMPTS,
		backOff = @BackOff(delay = RETRY_DELAY_MILLIS, multiplier = RETRY_MULTIPLIER),
		dltTopicSuffix = DLT_TOPIC_SUFFIX
	)
	public void handleSellerChanged(String payload) {
		productFacade.syncSeller(jsonConverter.fromJson(payload, SellerChangedEvent.class));
	}
}
