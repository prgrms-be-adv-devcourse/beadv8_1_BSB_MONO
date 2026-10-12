package com.bukang.order.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

// 임시 어댑터(FakeSeatHoldAdapter) 대신 테스트가 남은 자리를 정하는 자리 확보를 쓴다
@TestConfiguration
public class StubSeatHoldPortConfig {
	@Bean
	@Primary
	public StubSeatHoldPort stubSeatHoldPort() {
		return new StubSeatHoldPort();
	}
}
