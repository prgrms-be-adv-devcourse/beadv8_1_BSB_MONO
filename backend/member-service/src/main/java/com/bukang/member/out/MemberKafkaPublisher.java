package com.bukang.member.out;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bukang.common.global.json.JsonConverter;
import com.bukang.common.shared.kafka.KafkaTopics;
import com.bukang.common.shared.member.event.MemberJoinedEvent;
import com.bukang.common.shared.member.event.MemberModifiedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회원 엔티티가 발행한 스프링 이벤트를 커밋 뒤에 Kafka로 보낸다 (가입 BE-011, 수정 BE-015)
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

	// 키를 가입 이벤트와 같은 회원 ID로 둬 같은 파티션에 넣는다 (같은 회원의 가입·수정 순서 유지)
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleMemberModified(MemberModifiedEvent event) {
		send(KafkaTopics.MEMBER_MODIFIED, String.valueOf(event.id()), event);
	}

	// 가입·수정은 이미 커밋됐으므로 보내지 못해도 예외를 던지지 않고 에러 로그만 남긴다
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
