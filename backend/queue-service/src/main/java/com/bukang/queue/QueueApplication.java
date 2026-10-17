package com.bukang.queue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.bukang.queue.config.QueueProperties;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;

// common 모듈의 빈(GlobalExceptionHandler, JsonConverter)도 등록되도록 com.bukang.common을 함께 스캔한다
@SpringBootApplication(scanBasePackages = {"com.bukang.queue", "com.bukang.common"})
@EnableJpaAuditing
@EnableScheduling // 대기열 승격(QueuePromotionScheduler, test 프로파일 제외)
@EnableConfigurationProperties(QueueProperties.class)
public class QueueApplication {

	public static void main(String[] args) {
		SpringApplication.run(QueueApplication.class, args);
	}

	@Bean
	public JPAQueryFactory queryFactory(EntityManager em) {
		return new JPAQueryFactory(em);
	}

}
