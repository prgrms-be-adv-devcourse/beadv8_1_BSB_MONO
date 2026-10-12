package com.bukang.member.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RefreshTokenRepositoryTest {
	private StringRedisTemplate redisTemplate;
	private ValueOperations<String, String> valueOperations;
	private RefreshTokenRepository refreshTokenRepository;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		redisTemplate = mock(StringRedisTemplate.class);
		valueOperations = mock(ValueOperations.class);
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		refreshTokenRepository = new RefreshTokenRepository(redisTemplate);
	}

	@Test
	@DisplayName("Refresh Token을 members:{회원 ID}:refreshToken 키에 유효 기간을 TTL로 걸어 저장한다")
	void save() {
		refreshTokenRepository.save(7, "refresh-token", Duration.ofDays(14));

		verify(valueOperations).set("members:7:refreshToken", "refresh-token", Duration.ofDays(14));
	}

	@Test
	@DisplayName("저장된 토큰이 없으면 빈 값을 돌려준다")
	void findByMemberIdWhenAbsent() {
		given(valueOperations.get("members:7:refreshToken")).willReturn(null);

		assertThat(refreshTokenRepository.findByMemberId(7)).isEmpty();
	}
}
