package com.bukang.queue;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;

import com.redis.testcontainers.RedisContainer;

/**
 * 테스트용 Redis 컨테이너. 통합 테스트(QueueIntegrationTest)에 @Import로 붙인다.
 * 대기열의 ZSet·Lua는 가짜(mock) Redis로 검증할 수 없어 진짜 Redis를 띄운다 (Docker 필요, compose와 같은 버전).
 * 접속 주소(spring.data.redis.host, port)는 @ServiceConnection이 넣어 준다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {
	@Bean
	@ServiceConnection
	RedisContainer redisContainer() {
		return new RedisContainer("redis:8.2");
	}
}
