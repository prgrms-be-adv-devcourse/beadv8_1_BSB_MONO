package com.bukang.gateway.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;

import com.bukang.gateway.auth.GatewayAuthProperties.PublicPath;

class PublicPathsTest {

	private final PublicPaths publicPaths = new PublicPaths(new GatewayAuthProperties(List.of(
		new PublicPath("/api/v1/auth/login", null),
		new PublicPath("/api/v1/product/**", Set.of(HttpMethod.GET)),
		new PublicPath("/api/v1/member/members/email-availability", Set.of(HttpMethod.GET))
	)));

	@Test
	@DisplayName("패턴에 /**가 없으면 정확히 같은 주소만 공개로 본다")
	void exactPatternMatchesOnlySamePath() {
		assertThat(matches(HttpMethod.POST, "/api/v1/auth/login")).isTrue();
		assertThat(matches(HttpMethod.POST, "/api/v1/auth/login/")).isFalse();
		assertThat(matches(HttpMethod.POST, "/api/v1/auth/login-history")).isFalse();
		assertThat(matches(HttpMethod.POST, "/api/v1/auth/login/extra")).isFalse();
	}

	@Test
	@DisplayName("methods를 비우면 모든 메서드를 공개로 본다")
	void emptyMethodsMatchesAllMethods() {
		assertThat(matches(HttpMethod.GET, "/api/v1/auth/login")).isTrue();
		assertThat(matches(HttpMethod.DELETE, "/api/v1/auth/login")).isTrue();
	}

	@Test
	@DisplayName("/**는 그 아래 주소 전부를 공개로 보되, 정한 메서드만 통과시킨다")
	void wildcardPatternMatchesSubPathsWithAllowedMethod() {
		assertThat(matches(HttpMethod.GET, "/api/v1/product/races")).isTrue();
		assertThat(matches(HttpMethod.GET, "/api/v1/product/races/1/courses")).isTrue();
		assertThat(matches(HttpMethod.POST, "/api/v1/product/races")).isFalse();
		assertThat(matches(HttpMethod.GET, "/api/v1/products")).isFalse();
	}

	@Test
	@DisplayName("이메일 중복 확인은 GET만 공개로 보고, 같은 부모의 다른 회원 주소는 공개로 보지 않는다")
	void emailAvailabilityIsPublicOnlyForGet() {
		assertThat(matches(HttpMethod.GET, "/api/v1/member/members/email-availability")).isTrue();
		assertThat(matches(HttpMethod.POST, "/api/v1/member/members/email-availability")).isFalse();
		assertThat(matches(HttpMethod.GET, "/api/v1/member/members/me")).isFalse();
	}

	@Test
	@DisplayName("설정이 비어 있으면 어떤 주소도 공개로 보지 않는다")
	void emptyConfigMatchesNothing() {
		PublicPaths none = new PublicPaths(new GatewayAuthProperties(null));

		assertThat(none.matches(HttpMethod.POST, PathContainer.parsePath("/api/v1/auth/login"))).isFalse();
	}

	@Test
	@DisplayName("pattern이 비어 있으면 게이트웨이가 뜰 때 실패한다")
	void blankPatternFailsAtStartup() {
		assertThatThrownBy(() -> new PublicPath(" ", null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	private boolean matches(HttpMethod method, String path) {
		return publicPaths.matches(method, PathContainer.parsePath(path));
	}
}
