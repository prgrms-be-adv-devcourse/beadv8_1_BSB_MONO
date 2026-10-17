package com.bukang.order.out.queue;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bukang.order.out.InternalHttpClients;

/**
 * 대기열 서비스를 실제 HTTP로 부를 때만 켠다 (order.clients.queue.mode=http, PRO-226)
 * 기본값(fake)이면 FakeQueueAdmissionAdapter가 대신 답해, 대기열을 띄우지 않고도 주문을 개발할 수 있다
 * 대기열은 Redis를 2초까지 기다리므로, 주문은 그보다 조금 더 기다린다 (order.clients.queue.timeout)
 */
@Configuration
@ConditionalOnProperty(prefix = "order.clients.queue", name = "mode", havingValue = "http")
public class QueueClientConfig {
	@Bean
	public HttpQueueAdmissionAdapter httpQueueAdmissionAdapter(
		@Value("${order.clients.queue.base-url}") String baseUrl,
		@Value("${order.clients.queue.timeout:3s}") Duration timeout
	) {
		return new HttpQueueAdmissionAdapter(InternalHttpClients.create(baseUrl, timeout));
	}
}
