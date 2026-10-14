package com.bukang.member.config.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class RefreshTokenPropertiesTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withUserConfiguration(Config.class);

	@Test
	@DisplayName("설정이 없으면 유예 5초, 최대 30일을 쓴다")
	void usesDefaults() {
		runner.run(context -> {
			RefreshTokenProperties properties = context.getBean(RefreshTokenProperties.class);

			assertThat(properties.reuseGrace()).isEqualTo(Duration.ofSeconds(5));
			assertThat(properties.maxLifetime()).isEqualTo(Duration.ofDays(30));
		});
	}

	@Test
	@DisplayName("member.auth.refresh-token.* 값을 읽는다")
	void bindsFromProperties() {
		runner.withPropertyValues(
				"member.auth.refresh-token.reuse-grace=10s",
				"member.auth.refresh-token.max-lifetime=7d")
			.run(context -> {
				RefreshTokenProperties properties = context.getBean(RefreshTokenProperties.class);

				assertThat(properties.reuseGrace()).isEqualTo(Duration.ofSeconds(10));
				assertThat(properties.maxLifetime()).isEqualTo(Duration.ofDays(7));
			});
	}

	@Test
	@DisplayName("최대 기간이 0이거나 유예가 음수면 시작할 때 실패한다")
	void failsAtStartupWithInvalidValues() {
		runner.withPropertyValues("member.auth.refresh-token.max-lifetime=0s")
			.run(context -> assertThat(context).hasFailed());
		runner.withPropertyValues("member.auth.refresh-token.reuse-grace=-1s")
			.run(context -> assertThat(context).hasFailed());
	}

	@Configuration
	@EnableConfigurationProperties(RefreshTokenProperties.class)
	static class Config {
	}
}
