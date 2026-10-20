package com.bukang.file.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * S3 접속 설정 (file.storage.s3)
 * endpoint가 빈 문자열이면 실제 AWS S3를, 주소가 있으면 그 주소(로컬 S3Mock 등)를 쓴다.
 * 빈 문자열로 만들려면 .env에 S3_ENDPOINT=처럼 빈 값을 적는다. 줄이 없으면 application.yaml 기본값(S3Mock 주소)이 들어온다.
 * accessKey·secretKey가 둘 다 있으면 그 키를, 하나라도 빈 문자열이면 SDK 기본 자격 증명 체인(환경변수, ~/.aws, EC2 역할 등)을 쓴다.
 * 키도 .env에 줄이 없으면 application.yaml 기본값(S3Mock용 가짜 키 s3mock)이 들어온다.
 * prod는 키 기본값이 빈 문자열이라, 줄이 없으면 SDK 기본 자격 증명 체인을 쓴다.
 */
@Validated
@ConfigurationProperties(prefix = "file.storage.s3")
public record S3Properties(
	@NotBlank
	String bucket,
	// 키 앞에 붙이는 환경 이름 (dev, prod). dev와 prod가 같은 버킷을 쓰므로 dev/uploads/…, prod/files/…처럼 나눈다
	// 프로파일별 yaml에 적는다. '/'가 들어가면 키 계층이 어긋나므로 소문자·숫자·하이픈만 받는다
	@NotBlank
	@Pattern(regexp = "[a-z0-9-]+")
	String keyPrefix,
	@NotBlank
	String region,
	String endpoint,
	// presigned URL에 넣을 주소. 앱은 컨테이너 안 주소로 접속하지만 브라우저는 다른 주소로 접속할 때 쓴다
	String publicEndpoint,
	boolean pathStyle,
	String accessKey,
	String secretKey,
	@NotNull
	Duration uploadUrlExpiration,
	@NotNull
	Duration downloadUrlExpiration
) {
}
