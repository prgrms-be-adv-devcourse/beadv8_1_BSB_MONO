package com.bukang.order.out.payout;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PayoutClientConfigTest {
	// 실제 앱처럼 "2s" 같은 기간 값을 Duration으로 바꾸는 Spring Boot 변환기를 쓴다
	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withInitializer(context -> context.getBeanFactory()
			.setConversionService(ApplicationConversionService.getSharedInstance()))
		.withUserConfiguration(PayoutClientConfig.class, FakeRefundEligibilityAdapter.class)
		.withPropertyValues("order.clients.timeout=2s", "order.clients.payout.base-url=http://localhost:8082");

	@Test
	@DisplayName("mode가 http면 실제 정산 서비스를 부르는 어댑터만 등록된다")
	void http() {
		runner.withPropertyValues("order.clients.payout.mode=http").run(context ->
			assertThat(context).getBean(RefundEligibilityPort.class).isInstanceOf(HttpRefundEligibilityAdapter.class));
	}

	@Test
	@DisplayName("mode가 fake이거나 값이 없으면 가짜 어댑터만 등록된다")
	void fake() {
		runner.withPropertyValues("order.clients.payout.mode=fake").run(context ->
			assertThat(context).getBean(RefundEligibilityPort.class).isInstanceOf(FakeRefundEligibilityAdapter.class));
		runner.run(context ->
			assertThat(context).getBean(RefundEligibilityPort.class).isInstanceOf(FakeRefundEligibilityAdapter.class));
	}
}
