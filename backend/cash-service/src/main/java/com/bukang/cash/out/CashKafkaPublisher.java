package com.bukang.cash.out;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.bukang.common.global.json.JsonConverter;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CashKafkaPublisher {
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final JsonConverter jsonConverter;

	public void publish(String topic, String key, Object event) {
		String message = jsonConverter.toJson(event);

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					kafkaTemplate.send(topic, key, message);
				}
			});
			return;
		}
		kafkaTemplate.send(topic, key, message);
	}
}
