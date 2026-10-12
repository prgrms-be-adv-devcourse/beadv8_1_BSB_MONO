package com.bukang.product.out;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bukang.common.global.json.JsonConverter;
import com.bukang.common.shared.kafka.KafkaTopics;
import com.bukang.product.app.RaceSavedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 대회 저장이 커밋된 뒤 file-service에 이미지 연결 메시지를 보낸다 (race.file.link.requested)
 * 커밋 뒤에만 보내므로, 롤백된 대회에 파일이 연결되는 일은 없다.
 * 보내지 못하면 이미 커밋된 대회를 되돌릴 수 없어 에러 로그에 메시지를 남긴다.
 * 연결되지 않은 파일은 하루 뒤 file-service 정리 배치가 지우므로, 그 전에 로그의 메시지를 다시 보내야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileLinkPublisher {
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final JsonConverter jsonConverter;

	// 같은 대회의 메시지가 순서대로 처리되도록 대회 ID를 key로 보낸다 (같은 key는 같은 파티션)
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void publish(RaceSavedEvent event) {
		if (event.fileLink() == null) {
			return;
		}
		String key = String.valueOf(event.raceId());
		String payload = jsonConverter.toJson(event.fileLink());
		try {
			kafkaTemplate.send(KafkaTopics.RACE_FILE_LINK_REQUESTED, key, payload)
				.whenComplete((result, exception) -> {
					if (exception != null) {
						log.error("파일 연결 메시지를 보내지 못했습니다. raceId={}, payload={}", key, payload, exception);
					}
				});
		} catch (RuntimeException exception) {
			// 브로커에 연결하지 못하면 send()가 기다리다(max.block.ms, 기본 60초) 예외를 던진다. 커밋 뒤라 여기서 잡는다
			log.error("파일 연결 메시지를 보내지 못했습니다. raceId={}, payload={}", key, payload, exception);
		}
	}
}
