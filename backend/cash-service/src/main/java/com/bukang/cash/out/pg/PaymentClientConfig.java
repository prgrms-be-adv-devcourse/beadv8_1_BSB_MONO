package com.bukang.cash.out.pg;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Toss페이먼츠를 실제 HTTP로 부를 때만 켠다 (cash.clients.pg.mode=http, PRO-81)
 * 기본값(fake)이면 FakePaymentClient가 대신 답해, Toss 서버 없이도 충전을 개발할 수 있다
 */
@Configuration
@ConditionalOnProperty(prefix = "cash.clients.pg", name = "mode", havingValue = "http")
public class PaymentClientConfig {
	@Bean
	public TossPaymentClient tossPaymentClient(
		@Value("${cash.clients.pg.base-url}") String baseUrl,
		@Value("${cash.clients.pg.secret-key}") String secretKey,
		@Value("${cash.clients.pg.timeout}") Duration timeout
	) {
		HttpClient httpClient = HttpClient.newBuilder()
			.version(HttpClient.Version.HTTP_1_1)
			.connectTimeout(timeout)
			.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(timeout);

		RestClient restClient = RestClient.builder()
			.baseUrl(baseUrl)
			.requestFactory(requestFactory)
			.build();
		return new TossPaymentClient(restClient, secretKey);
	}
}
