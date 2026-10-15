package com.bukang.order.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

// 가짜 어댑터(FakeQueueAdmissionAdapter) 대신 테스트가 대기열 입장 여부를 정한다
@TestConfiguration
public class StubQueueAdmissionPortConfig {
	@Bean
	@Primary
	public StubQueueAdmissionPort stubQueueAdmissionPort() {
		return new StubQueueAdmissionPort();
	}
}
