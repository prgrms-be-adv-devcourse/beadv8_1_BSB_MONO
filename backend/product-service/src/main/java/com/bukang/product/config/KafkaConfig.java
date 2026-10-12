package com.bukang.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {
	private static final long RETRY_INTERVAL_MILLIS = 1000L;
	private static final long MAX_RETRIES = 2L;

	// Spring Boot가 리스너 컨테이너에 자동으로 적용한다 (CommonErrorHandler 빈)
	@Bean
	public DefaultErrorHandler kafkaErrorHandler() {
		// 그 밖의 예외(회원 복제본보다 판매자 메시지를 먼저 읽어 외래 키가 실패하는 경우 등)는
		// 1초 간격으로 2번 더 시도한 뒤 로그를 남기고 다음 메시지로 넘어간다
		FixedBackOff backOff = new FixedBackOff(RETRY_INTERVAL_MILLIS, MAX_RETRIES);
		DefaultErrorHandler errorHandler = new DefaultErrorHandler(backOff);
		// 깨진 JSON, 모르는 Enum 값은 다시 해도 실패하므로 재시도 없이 건너뛴다
		// (JsonConverter가 파싱 실패를 IllegalArgumentException으로 바꾼다)
		errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
		return errorHandler;
	}
}
