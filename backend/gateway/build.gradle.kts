// API 게이트웨이: Nginx 뒤에서 /api 요청을 받아 경로에 맞는 서비스로 넘긴다
// WebFlux(Netty) 기반이라 webmvc를 가져오는 common 모듈은 넣지 않는다
plugins {
	java
	id("org.springframework.boot")
	id("io.spring.dependency-management")
}

// Spring Cloud 2025.1.x가 Spring Boot 4.x 대응 버전이다 (Boot 4.1.x는 2025.1.2 이상)
extra["springCloudVersion"] = "2025.1.3"

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
	}
}

dependencies {
	implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
}
