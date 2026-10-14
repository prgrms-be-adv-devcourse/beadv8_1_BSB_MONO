package com.bukang.gateway.auth;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * gateway.auth.public-paths 설정으로 "토큰 없이 통과하는 주소인가"를 판단한다.
 * 패턴은 시작할 때 한 번만 해석하므로, 잘못 쓴 패턴은 요청 때가 아니라 게이트웨이가 뜰 때 실패한다.
 */
@Component
public class PublicPaths {

	private final List<Rule> rules;

	public PublicPaths(GatewayAuthProperties properties) {
		PathPatternParser parser = PathPatternParser.defaultInstance;
		this.rules = properties.publicPaths().stream()
			.map(publicPath -> new Rule(parser.parse(publicPath.pattern()), publicPath.methods()))
			.toList();
	}

	public boolean matches(HttpMethod method, PathContainer path) {
		return rules.stream().anyMatch(rule -> rule.matches(method, path));
	}

	private record Rule(PathPattern pattern, Set<HttpMethod> methods) {

		boolean matches(HttpMethod method, PathContainer path) {
			return (methods.isEmpty() || methods.contains(method)) && pattern.matches(path);
		}
	}
}
