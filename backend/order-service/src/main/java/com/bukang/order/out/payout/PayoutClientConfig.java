package com.bukang.order.out.payout;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bukang.order.out.InternalHttpClients;

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
		return new HttpRefundEligibilityAdapter(InternalHttpClients.create(baseUrl, timeout));
	}
}
