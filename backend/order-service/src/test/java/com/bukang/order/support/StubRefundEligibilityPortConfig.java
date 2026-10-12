package com.bukang.order.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

// 임시 어댑터(FakeRefundEligibilityAdapter) 대신 테스트가 환불 가능 여부를 정한다
@TestConfiguration
public class StubRefundEligibilityPortConfig {
	@Bean
	@Primary
	public StubRefundEligibilityPort stubRefundEligibilityPort() {
		return new StubRefundEligibilityPort();
	}
}
