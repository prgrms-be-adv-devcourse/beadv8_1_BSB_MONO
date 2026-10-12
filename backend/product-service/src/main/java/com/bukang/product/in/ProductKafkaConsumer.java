package com.bukang.product.in;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.bukang.common.global.json.JsonConverter;
import com.bukang.common.shared.kafka.KafkaTopics;
import com.bukang.common.shared.member.event.MemberJoinedEvent;
import com.bukang.common.shared.seller.event.SellerChangedEvent;
import com.bukang.product.app.ProductFacade;

import lombok.RequiredArgsConstructor;

/**
 * 회원·판매자 이벤트를 받아 복제본(ProductMember, ProductSeller)에 저장한다
 * 메시지는 JSON 문자열로 받아 JsonConverter로 바꾼다. 파싱에 실패하면 Spring Kafka 기본 에러 핸들러가 10번 시도한 뒤 건너뛴다.
 */
@Component
@RequiredArgsConstructor
public class ProductKafkaConsumer {
	// application.yaml에 group-id가 없으므로 리스너마다 적는다
	private static final String GROUP_ID = "product";

	private final ProductFacade productFacade;
	private final JsonConverter jsonConverter;

	@KafkaListener(topics = KafkaTopics.PRODUCT_MEMBER_JOINED, groupId = GROUP_ID)
	public void handleMemberJoined(String payload) {
		productFacade.syncMember(jsonConverter.fromJson(payload, MemberJoinedEvent.class));
	}

	@KafkaListener(topics = KafkaTopics.PRODUCT_SELLER_CHANGED, groupId = GROUP_ID)
	public void handleSellerChanged(String payload) {
		productFacade.syncSeller(jsonConverter.fromJson(payload, SellerChangedEvent.class));
	}
}
