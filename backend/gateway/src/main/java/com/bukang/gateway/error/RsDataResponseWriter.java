package com.bukang.gateway.error;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;

import reactor.core.publisher.Mono;

/**
 * 게이트웨이가 직접 보내는 오류 응답을 RsData {status, code, message, data} 형식의 JSON으로 쓴다.
 * 게이트웨이는 common(webmvc)에 의존하지 않으므로 RsData 대신 JSON 문자열을 직접 만든다.
 */
public final class RsDataResponseWriter {

	private RsDataResponseWriter() {
	}

	// code와 message는 호출하는 클래스의 고정 상수·문구만 들어오므로 JSON 이스케이프가 필요 없다
	public static Mono<Void> write(ServerHttpResponse response, HttpStatus status, String code, String message) {
		String body = "{\"status\":%d,\"code\":\"%s\",\"message\":\"%s\",\"data\":null}"
			.formatted(status.value(), code, message);
		DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
		response.setStatusCode(status);
		response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		return response.writeWith(Mono.just(buffer));
	}
}
