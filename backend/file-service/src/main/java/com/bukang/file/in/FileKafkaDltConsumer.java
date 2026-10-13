package com.bukang.file.in;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.bukang.common.shared.kafka.KafkaTopics;

import lombok.extern.slf4j.Slf4j;

/**
 * 재시도를 모두 실패했거나 재시도할 필요가 없는 실패로 DLT(<토픽>.dlt)에 간 메시지를 받아 에러 로그로 남긴다
 * 다시 처리하지 않는다. 원인을 고친 뒤 필요하면 Kafka Console(http://localhost:8091)에서 메시지를 확인해 다시 보낸다.
 * 연결되지 않은 파일은 하루 뒤 정리 배치가 지우므로, 다시 보내려면 그 전에 보내야 한다.
 */
@Slf4j
@Component
public class FileKafkaDltConsumer {
	// 본 리스너(file)와 오프셋을 따로 관리한다
	private static final String GROUP_ID = "file-dlt";
	private static final String FILE_LINK_REQUESTED_DLT = KafkaTopics.RACE_FILE_LINK_REQUESTED
		+ FileKafkaConsumer.DLT_TOPIC_SUFFIX;

	@KafkaListener(topics = FILE_LINK_REQUESTED_DLT, groupId = GROUP_ID)
	public void handleFileLinkRequestedDlt(String payload) {
		log.error("처리하지 못한 파일 연결 메시지: {}", payload);
	}
}
