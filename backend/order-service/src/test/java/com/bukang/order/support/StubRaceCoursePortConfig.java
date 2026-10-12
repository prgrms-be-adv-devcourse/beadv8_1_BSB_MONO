package com.bukang.order.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

// 임시 어댑터(FakeRaceCourseAdapter) 대신 테스트가 값을 정하는 종목 정보를 쓴다
@TestConfiguration
public class StubRaceCoursePortConfig {
	@Bean
	@Primary
	public StubRaceCoursePort stubRaceCoursePort() {
		return new StubRaceCoursePort();
	}
}
