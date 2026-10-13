package com.bukang.product;

import java.nio.file.Path;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

/**
 * 테스트용 Elasticsearch 컨테이너. 테스트 클래스에 @Import로 붙인다.
 * 공식 이미지에는 nori가 없어 race-settings.json의 분석기로 인덱스를 만들 수 없으므로,
 * compose와 같은 Dockerfile(backend/docker/elasticsearch)로 이미지를 만든다. 이미지는 지우지 않아 다음 실행부터 재사용된다.
 * 접속 주소(spring.elasticsearch.uris)는 @ServiceConnection이 넣어 준다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {
	private static final String IMAGE_NAME = "bukang-elasticsearch-test:9.4.5-nori";
	// 테스트 실행 위치(product-service 모듈 폴더) 기준
	private static final Path DOCKERFILE = Path.of("../docker/elasticsearch/Dockerfile");

	@Bean
	@ServiceConnection
	ElasticsearchContainer elasticsearchContainer() {
		String image = new ImageFromDockerfile(IMAGE_NAME, false)
			.withDockerfile(DOCKERFILE)
			.get();
		return new ElasticsearchContainer(DockerImageName.parse(image)
			.asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch"))
			// compose와 같은 로컬 개발용 설정 (보안 끔, 힙 512MB)
			.withEnv("xpack.security.enabled", "false")
			.withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");
	}
}
