package com.bukang.common.global.exception;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import com.bukang.common.global.rsdata.RsData;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 어느 핸들러도 처리하지 못한 예외를 RsData 형식의 500(INTERNAL_ERROR)으로 응답한다.
 * Spring MVC의 기본 예외 처리(404, 405 등)와 모든 @RestControllerAdvice보다 뒤에 실행되도록 가장 마지막 순서에 둔다.
 * @ExceptionHandler(Exception.class)로 만들면 다른 @RestControllerAdvice(예: 서비스별 헤더 누락 처리)보다
 * 먼저 골라질 수 있고, 404·405 같은 예외까지 500으로 바뀌므로 HandlerExceptionResolver로 만든다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnexpectedExceptionResolver implements HandlerExceptionResolver, Ordered {
	private static final String INTERNAL_ERROR = "INTERNAL_ERROR";
	private static final String MESSAGE = "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";

	private final ObjectMapper objectMapper;

	@Override
	public ModelAndView resolveException(
		HttpServletRequest request,
		HttpServletResponse response,
		Object handler,
		Exception exception
	) {
		if (response.isCommitted()) {
			return null;
		}

		log.error("처리하지 못한 예외: {} {}", request.getMethod(), request.getRequestURI(), exception);

		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		try {
			String body = objectMapper.writeValueAsString(RsData.of(status, INTERNAL_ERROR, MESSAGE, null));
			response.setStatus(status.value());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.setCharacterEncoding(StandardCharsets.UTF_8.name());
			response.getWriter().write(body);
		} catch (IOException | JacksonException writeException) {
			// 응답을 쓰지 못하면 Spring 기본 처리(/error)로 넘긴다
			log.warn("500 응답을 쓰지 못했습니다.", writeException);
			return null;
		}
		return new ModelAndView();
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}
}
