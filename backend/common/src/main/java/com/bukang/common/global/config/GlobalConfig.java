package com.bukang.common.global.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.bukang.common.global.eventpublisher.EventPublisher;

import lombok.Getter;

/**
 * 스프링 빈이 아닌 엔티티에서도 이벤트를 발행할 수 있게 EventPublisher를 정적 필드로 꺼내 둔다.
 * 엔티티는 BaseEntity.publishEvent로 발행한다.
 */
@Configuration
public class GlobalConfig {
	@Getter
	private static EventPublisher eventPublisher;

	@Autowired
	public void setEventPublisher(EventPublisher eventPublisher) {
		GlobalConfig.eventPublisher = eventPublisher;
	}
}
