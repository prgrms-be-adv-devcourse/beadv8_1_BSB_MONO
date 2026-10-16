package com.bukang.gateway.auth;

import java.nio.charset.StandardCharsets;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.bukang.jwt.InvalidTokenException;
import com.bukang.jwt.JwtClaims;
import com.bukang.jwt.JwtProvider;
import com.bukang.jwt.TokenType;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * 라우팅되는 모든 요청의 Access Token을 검사하고, 통과하면 토큰 속 회원 ID를 X-Member-Id 헤더로 서비스에 넘긴다.
 * 서비스는 이 헤더를 로그인한 회원으로 믿으므로, 클라이언트가 보낸 X-Member-Id는 검사 전에 무조건 지운다 (위조 차단).
 * 라우팅 규칙에 맞지 않는 요청(/internal/** 등)은 이 필터를 거치지 않고 404로 응답한다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
	public static final String MEMBER_ID_HEADER = "X-Member-Id";

	private static final String BEARER_PREFIX = "Bearer ";

	// 401 응답의 code. 프론트가 오류 원인을 구분하는 값이라 이 한곳에서만 정의한다
	private static final String CODE_LOGIN_REQUIRED = "AUTH_LOGIN_REQUIRED";
	private static final String CODE_TOKEN_EXPIRED = "AUTH_TOKEN_EXPIRED";
	private static final String CODE_INVALID_TOKEN = "AUTH_INVALID_TOKEN";

	private final JwtProvider jwtProvider;
	// 토큰 없이 통과하는 주소는 코드가 아니라 application.yaml의 gateway.auth.public-paths에 둔다
	private final PublicPaths publicPaths;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest().mutate()
			.headers(headers -> headers.remove(MEMBER_ID_HEADER))
			.build();

		String token = resolveToken(request);
		if (isPublic(request)) {
			// 공개 주소라도 유효한 토큰이 있으면 회원 ID를 넘긴다 (예: 대회 상세의 "이미 신청함" 표시)
			// 토큰이 없거나 무효하면 로그인하지 않은 요청으로 넘긴다
			ServerHttpRequest forwarded = withMemberIdIfValid(request, token);
			return chain.filter(exchange.mutate().request(forwarded).build());
		}

		if (token == null) {
			return unauthorized(exchange.getResponse(), CODE_LOGIN_REQUIRED, "로그인이 필요합니다.");
		}

		JwtClaims claims;
		try {
			claims = jwtProvider.parse(token, TokenType.ACCESS);
		} catch (InvalidTokenException e) {
			// 만료는 클라이언트가 재발급을 시도할 수 있도록 코드와 메시지를 구분한다
			if (e.getReason() == InvalidTokenException.Reason.EXPIRED) {
				return unauthorized(exchange.getResponse(), CODE_TOKEN_EXPIRED, "만료된 토큰입니다.");
			}
			return unauthorized(exchange.getResponse(), CODE_INVALID_TOKEN, "유효하지 않은 토큰입니다.");
		}

		ServerHttpRequest authenticated = request.mutate()
			.header(MEMBER_ID_HEADER, String.valueOf(claims.memberId()))
			.build();
		return chain.filter(exchange.mutate().request(authenticated).build());
	}

	// 토큰 검사가 다른 필터보다 먼저 일어나도록 가장 앞에 둔다
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	// CORS 사전 요청(OPTIONS)은 브라우저가 토큰 없이 보내므로 통과시킨다
	// OPTIONS는 API마다 다른 규칙이 아니라 브라우저 동작이라 설정(gateway.auth.public-paths)이 아닌 코드에 둔다
	private boolean isPublic(ServerHttpRequest request) {
		return HttpMethod.OPTIONS.equals(request.getMethod())
			|| publicPaths.matches(request.getMethod(), request.getPath().pathWithinApplication());
	}

	private ServerHttpRequest withMemberIdIfValid(ServerHttpRequest request, String token) {
		if (token == null) {
			return request;
		}
		try {
			JwtClaims claims = jwtProvider.parse(token, TokenType.ACCESS);
			return request.mutate().header(MEMBER_ID_HEADER, String.valueOf(claims.memberId())).build();
		} catch (InvalidTokenException e) {
			return request;
		}
	}

	private String resolveToken(ServerHttpRequest request) {
		String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
			return null;
		}
		String token = authorization.substring(BEARER_PREFIX.length()).trim();
		return token.isEmpty() ? null : token;
	}

	// 게이트웨이는 common(webmvc)에 의존하지 않으므로 RsData {status, code, message, data} 형식의 JSON을 직접 만든다
	// code와 message는 이 클래스의 고정 상수·문구만 들어오므로 JSON 이스케이프가 필요 없다
	private Mono<Void> unauthorized(ServerHttpResponse response, String code, String message) {
		HttpStatus status = HttpStatus.UNAUTHORIZED;
		String body = "{\"status\":%d,\"code\":\"%s\",\"message\":\"%s\",\"data\":null}"
			.formatted(status.value(), code, message);
		DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
		response.setStatusCode(status);
		response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		return response.writeWith(Mono.just(buffer));
	}
}
