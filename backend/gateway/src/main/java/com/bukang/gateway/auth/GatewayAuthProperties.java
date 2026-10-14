package com.bukang.gateway.auth;

import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;

/**
 * 게이트웨이 인증 설정 (gateway.auth.*).
 *
 * @param publicPaths 토큰 없이 통과하는 주소. 비어 있으면 모든 주소가 토큰을 요구한다
 */
@ConfigurationProperties("gateway.auth")
public record GatewayAuthProperties(List<PublicPath> publicPaths) {

	public GatewayAuthProperties {
		publicPaths = publicPaths == null ? List.of() : List.copyOf(publicPaths);
	}

	/**
	 * 토큰 없이 통과하는 주소 하나.
	 *
	 * @param pattern 주소 패턴. "/api/v1/auth/login"처럼 쓰면 그 주소와 정확히 같을 때만, "/api/v1/product/**"처럼 쓰면 그 아래 전부
	 * @param methods 통과시킬 HTTP 메서드. 비어 있으면 모든 메서드
	 */
	public record PublicPath(String pattern, Set<HttpMethod> methods) {

		public PublicPath {
			if (pattern == null || pattern.isBlank()) {
				throw new IllegalArgumentException("gateway.auth.public-paths[].pattern은 비어 있을 수 없습니다");
			}
			methods = methods == null ? Set.of() : Set.copyOf(methods);
		}
	}
}
