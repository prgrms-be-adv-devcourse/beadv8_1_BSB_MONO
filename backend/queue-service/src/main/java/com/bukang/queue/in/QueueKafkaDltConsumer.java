package com.bukang.queue.in;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.bukang.common.shared.kafka.KafkaTopics;

import lombok.extern.slf4j.Slf4j;

/**
 * 재시도를 모두 실패했거나 재시도할 필요가 없는 실패로 DLT(<토픽>.queue.dlt)에 간 메시지를 받아 에러 로그로 남긴다
 * 다시 처리하지 않는다. 원인을 고친 뒤 필요하면 Kafka Console(http://localhost:8091)에서 메시지를 확인해 다시 보낸다.
 */
@Slf4j
@Component
public class QueueKafkaDltConsumer {
	// 본 리스너(queue-service)와 오프셋을 따로 관리한다
	private static final String GROUP_ID = "queue-service-dlt";
	private static final String RACE_CHANGED_DLT = KafkaTopics.RACE_CHANGED
		+ QueueKafkaConsumer.DLT_TOPIC_SUFFIX;
	private static final String RACE_COURSE_CHANGED_DLT = KafkaTopics.RACE_COURSE_CHANGED
		+ QueueKafkaConsumer.DLT_TOPIC_SUFFIX;

	// 반영하지 못한 대회는 복제본이 옛 판매 상태·일정으로 남는다. 원인을 고친 뒤 메시지를 다시 보내야 한다
	@KafkaListener(topics = RACE_CHANGED_DLT, groupId = GROUP_ID)
	public void handleRaceChangedDlt(String payload) {
		log.error("처리하지 못한 대회 변경 메시지: {}", payload);
	}

	// 반영하지 못한 종목은 복제본에 없거나 뺀 종목이 남는다. 원인을 고친 뒤 메시지를 다시 보내야 한다
	@KafkaListener(topics = RACE_COURSE_CHANGED_DLT, groupId = GROUP_ID)
	public void handleRaceCourseChangedDlt(String payload) {
		log.error("처리하지 못한 종목 변경 메시지: {}", payload);
	}
}
