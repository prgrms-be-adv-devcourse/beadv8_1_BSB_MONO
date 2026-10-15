package com.bukang.common.global.eventpublisher;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * 스프링 애플리케이션 이벤트를 발행한다 (ApplicationEventPublisher를 감싼다)
 */
@Service
@RequiredArgsConstructor
public class EventPublisher {
	private final ApplicationEventPublisher applicationEventPublisher;

	public void publish(Object event) {
		applicationEventPublisher.publishEvent(event);
	}
}
