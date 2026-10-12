package com.bukang.order.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

// 임시 어댑터(FakeWalletAdapter) 대신 테스트가 잔액을 정하는 예치금을 쓴다
@TestConfiguration
public class StubWalletPortConfig {
	@Bean
	@Primary
	public StubWalletPort stubWalletPort() {
		return new StubWalletPort();
	}
}
