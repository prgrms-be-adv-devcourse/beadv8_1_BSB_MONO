package com.bukang.payout.support;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;

import org.springframework.boot.restdocs.test.autoconfigure.RestDocsMockMvcConfigurationCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * REST Docs 스니펫의 요청·응답 JSON을 줄 바꿈해 읽기 좋게 남긴다
 */
@TestConfiguration
public class RestDocsConfig {
	@Bean
	public RestDocsMockMvcConfigurationCustomizer restDocsPrettyPrint() {
		return configurer -> configurer.operationPreprocessors()
			.withRequestDefaults(prettyPrint())
			.withResponseDefaults(prettyPrint());
	}
}
