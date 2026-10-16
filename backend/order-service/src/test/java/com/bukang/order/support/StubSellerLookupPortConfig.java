package com.bukang.order.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

// 가짜 어댑터(FakeSellerLookupAdapter) 대신 테스트가 판매자를 정한다
@TestConfiguration
public class StubSellerLookupPortConfig {
	@Bean
	@Primary
	public StubSellerLookupPort stubSellerLookupPort() {
		return new StubSellerLookupPort();
	}
}
