package com.bukang.order.out.payout;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 정산 서비스(E)를 실제 HTTP로 부를 때만 켠다 (order.clients.payout.mode=http, PRO-99)
 * 기본값(fake)이면 FakeRefundEligibilityAdapter가 대신 답해, 정산 서비스를 띄우지 않고도 주문을 개발할 수 있다
 */
@Configuration
@ConditionalOnProperty(prefix = "order.clients.payout", name = "mode", havingValue = "http")
public class PayoutClientConfig {
	@Bean
	public HttpRefundEligibilityAdapter httpRefundEligibilityAdapter(
		@Value("${order.clients.payout.base-url}") String baseUrl,
		@Value("${order.clients.timeout}") Duration timeout
	) {
		// 정산 서비스가 느려도 취소 요청이 오래 붙잡히지 않게 연결·응답 모두 시간 제한을 둔다
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(timeout);
		RestClient restClient = RestClient.builder()
			.baseUrl(baseUrl)
			.requestFactory(requestFactory)
			.build();
		return new HttpRefundEligibilityAdapter(restClient);
	}
}
