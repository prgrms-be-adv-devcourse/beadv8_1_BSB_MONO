package com.bukang.member.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 지금 시각을 빈으로 받아, 테스트에서 고정한 시계로 바꿔 끼울 수 있게 한다 (Refresh Token 유예 5초, 상한 30일 계산)
@Configuration
public class ClockConfig {

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}
