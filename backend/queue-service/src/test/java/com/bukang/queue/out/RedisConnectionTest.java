package com.bukang.queue.out;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.redis.testcontainers.RedisContainer;

// 대기열의 ZSet·Lua는 가짜(mock) Redis로 검증할 수 없어 진짜 Redis 컨테이너로 확인한다 (Docker 필요)
// 이 테스트는 컨테이너에 연결되는지만 본다. 대기열 기능 테스트는 단계마다 따로 쓴다
@Testcontainers
class RedisConnectionTest {
	private static final String KEY = "tests:redis:connection";

	@Container
	private static final RedisContainer REDIS = new RedisContainer("redis:8.2");

	private static LettuceConnectionFactory connectionFactory;
	private static StringRedisTemplate redisTemplate;

	@BeforeAll
	static void connect() {
		connectionFactory = new LettuceConnectionFactory(REDIS.getRedisHost(), REDIS.getRedisPort());
		connectionFactory.afterPropertiesSet();
		connectionFactory.start();
		redisTemplate = new StringRedisTemplate(connectionFactory);
	}

	@AfterAll
	static void disconnect() {
		connectionFactory.destroy();
	}

	@Test
	@DisplayName("Redis 컨테이너에 SET한 값을 GET으로 다시 읽는다")
	void readsBackSetValue() {
		redisTemplate.opsForValue().set(KEY, "ok");

		assertThat(redisTemplate.opsForValue().get(KEY)).isEqualTo("ok");
	}
}
