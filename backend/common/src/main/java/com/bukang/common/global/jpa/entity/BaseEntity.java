package com.bukang.common.global.jpa.entity;

import java.time.LocalDateTime;

import com.bukang.common.global.config.GlobalConfig;
import com.bukang.common.standard.modeltype.HasModelTypeCode;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@MappedSuperclass
@Getter
// 모든 엔티티들의 조상
public abstract class BaseEntity implements HasModelTypeCode {
	public abstract int getId();

	public abstract LocalDateTime getCreateDate();

	public abstract LocalDateTime getModifyDate();

	@Override
	public String getModelTypeCode() {
		return this.getClass().getSimpleName();
	}

	// 엔티티 안에서 스프링 애플리케이션 이벤트를 발행한다 (GlobalConfig가 넣어 둔 EventPublisher 사용)
	protected void publishEvent(Object event) {
		GlobalConfig.getEventPublisher().publish(event);
	}

}
