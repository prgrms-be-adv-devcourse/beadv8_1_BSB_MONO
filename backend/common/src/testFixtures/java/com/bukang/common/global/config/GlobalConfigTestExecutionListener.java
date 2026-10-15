package com.bukang.common.global.config;

import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import com.bukang.common.global.eventpublisher.EventPublisher;

/**
 * 테스트 메서드마다 GlobalConfig의 정적 EventPublisher를 지금 테스트의 컨텍스트 것으로 다시 맞춘다.
 * 정적 필드는 마지막에 만들어진 컨텍스트를 가리키는데, 테스트는 컨텍스트를 캐시에 두고 다시 쓰므로
 * 엔티티 이벤트가 다른 컨텍스트로 가는 일이 생긴다. GlobalConfig나 EventPublisher 빈이 없으면 아무것도 하지 않는다.
 */
public class GlobalConfigTestExecutionListener implements TestExecutionListener {
	@Override
	public void beforeTestMethod(TestContext testContext) {
		ApplicationContext context = testContext.getApplicationContext();
		context.getBeanProvider(GlobalConfig.class).ifAvailable(globalConfig ->
			context.getBeanProvider(EventPublisher.class).ifAvailable(globalConfig::setEventPublisher));
	}
}
