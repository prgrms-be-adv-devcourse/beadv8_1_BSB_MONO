package com.bukang.member.out;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bukang.common.global.json.JsonConverter;
import com.bukang.common.shared.kafka.KafkaTopics;
import com.bukang.common.shared.member.event.MemberJoinedEvent;
import com.bukang.common.shared.seller.event.SellerChangedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회원·판매자 엔티티가 발행한 스프링 이벤트를 커밋 뒤에 Kafka로 보낸다 (BE-011)
 * 판매자 변경(신청, 승인·반려)은 BE-020에 따라 같은 구조로 보낸다
 * 트랜잭션 밖에서 발행된 이벤트는 받지 않는다 (fallbackExecution 끔)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberKafkaPublisher {
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final JsonConverter jsonConverter;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleMemberJoined(MemberJoinedEvent event) {
		send(KafkaTopics.MEMBER_MEMBER_JOINED, String.valueOf(event.id()), event);
	}

	// 키는 판매자 ID. 같은 판매자의 신청·심사 이벤트가 같은 파티션에 들어가 순서가 지켜진다
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleSellerChanged(SellerChangedEvent event) {
		send(KafkaTopics.SELLER_CHANGED, String.valueOf(event.id()), event);
	}

	// 저장은 이미 커밋됐으므로 보내지 못해도 예외를 던지지 않고 에러 로그만 남긴다
	private void send(String topic, String key, Object event) {
		String payload = jsonConverter.toJson(event);
		try {
			kafkaTemplate.send(topic, key, payload)
				.whenComplete((sendResult, exception) -> {
					if (exception != null) {
						log.error("Kafka 메시지를 보내지 못했습니다. topic={}, key={}", topic, key, exception);
					}
				});
		} catch (RuntimeException exception) {
			// 브로커에 연결하지 못하면 send()가 기다리다(max.block.ms, application.yaml에서 3초) 예외를 던진다
			log.error("Kafka 메시지를 보내지 못했습니다. topic={}, key={}", topic, key, exception);
		}
	}
}
