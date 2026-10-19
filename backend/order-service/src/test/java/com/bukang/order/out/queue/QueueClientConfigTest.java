package com.bukang.order.out.queue;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class QueueClientConfigTest {
	// 실제 앱처럼 "3s" 같은 기간 값을 Duration으로 바꾸는 Spring Boot 변환기를 쓴다
	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withInitializer(context -> context.getBeanFactory()
			.setConversionService(ApplicationConversionService.getSharedInstance()))
		.withUserConfiguration(QueueClientConfig.class, FakeQueueAdmissionAdapter.class)
		.withPropertyValues("order.clients.queue.base-url=http://localhost:8086");

	@Test
	@DisplayName("mode가 http면 실제 대기열을 부르는 어댑터만 등록된다 (제한 시간을 따로 정하지 않아도 뜬다)")
	void http() {
		runner.withPropertyValues("order.clients.queue.mode=http").run(context ->
			assertThat(context).getBean(QueueAdmissionPort.class).isInstanceOf(HttpQueueAdmissionAdapter.class));
	}

	@Test
	@DisplayName("mode가 fake이거나 값이 없으면 가짜 어댑터만 등록된다")
	void fake() {
		runner.withPropertyValues("order.clients.queue.mode=fake").run(context ->
			assertThat(context).getBean(QueueAdmissionPort.class).isInstanceOf(FakeQueueAdmissionAdapter.class));
		runner.run(context ->
			assertThat(context).getBean(QueueAdmissionPort.class).isInstanceOf(FakeQueueAdmissionAdapter.class));
	}

	@Test
	@DisplayName("운영(prod) 설정은 실제 대기열을 컨테이너 이름으로 부른다")
	void prodProfileUsesRealQueue() throws Exception {
		List<PropertySource<?>> prod = new YamlPropertySourceLoader()
			.load("prod", new ClassPathResource("application-prod.yaml"));

		assertThat(prod).anySatisfy(source -> {
			assertThat(source.getProperty("order.clients.queue.mode")).isEqualTo("http");
			assertThat(source.getProperty("order.clients.queue.base-url")).isEqualTo("http://bukang-queue-service:8086");
		});
	}
}
