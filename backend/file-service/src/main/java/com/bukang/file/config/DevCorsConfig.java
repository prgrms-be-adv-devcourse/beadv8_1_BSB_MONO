package com.bukang.file.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * dev 전용 CORS 설정. product-service의 대회 등록 테스트 페이지(8085)가 업로드 API를 부를 수 있게 한다.
 * 페이지 주소(8085)와 API 주소(8083)가 달라 브라우저가 먼저 허용 여부를 묻는데(preflight), 이 설정이 없으면 막힌다.
 * 실제 프론트는 API를 자기 주소(/api)로 프록시해서 이 설정이 필요 없으므로 prod에는 두지 않는다.
 */
@Configuration
@Profile("dev")
public class DevCorsConfig implements WebMvcConfigurer {
	private static final String PRODUCT_DEV_ORIGIN = "http://localhost:8085";

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/v1/file/files/**")
			.allowedOrigins(PRODUCT_DEV_ORIGIN)
			.allowedMethods("GET", "POST")
			.allowedHeaders("Content-Type", "X-Member-Id");
	}
}
