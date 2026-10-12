package com.bukang.member.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthTokensTest {

	@Test
	@DisplayName("toString()에는 토큰 값이 나오지 않는다")
	void toStringHidesTokens() {
		AuthTokens tokens = new AuthTokens("access-token-value", "refresh-token-value", Duration.ofDays(14));

		assertThat(tokens.toString())
			.doesNotContain("access-token-value")
			.doesNotContain("refresh-token-value");
	}
}
