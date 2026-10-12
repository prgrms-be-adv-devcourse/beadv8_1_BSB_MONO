package com.bukang.order.in;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.bukang.common.global.rsdata.RsData;

/**
 * 주문 서비스 전용 요청 예외 처리
 * 처리하지 않으면 /error로 넘어가 공통 응답 형식(RsData)이 아닌 기본 에러 응답이 나간다.
 */
@RestControllerAdvice
public class OrderExceptionHandler {
	// 회원 ID 헤더는 게이트웨이가 토큰을 검사해 넣어 줄 예정이다(PRO-20)
	private static final String MEMBER_ID_HEADER = "X-Member-Id";

	// 회원 ID 헤더가 없으면 로그인이 안 된 것이고, 그 밖의 헤더(예: 중복 요청 방지 키)가 없으면 잘못된 요청이다
	@ExceptionHandler(MissingRequestHeaderException.class)
	public ResponseEntity<RsData<Void>> missingRequestHeaderException(MissingRequestHeaderException exception) {
		if (MEMBER_ID_HEADER.equalsIgnoreCase(exception.getHeaderName())) {
			return ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.body(RsData.of(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.", null));
		}
		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(RsData.of(HttpStatus.BAD_REQUEST, "필수 요청 헤더(%s)가 없습니다.".formatted(exception.getHeaderName()), null));
	}

	// 필수 쿼리 파라미터 누락
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<RsData<Void>> missingServletRequestParameterException(
		MissingServletRequestParameterException exception
	) {
		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(RsData.of(HttpStatus.BAD_REQUEST, "필수 요청 값이 없습니다.", null));
	}

	// 경로 변수·헤더·쿼리 파라미터의 숫자 형식 오류 (예: 회원 ID에 문자)
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<RsData<Void>> methodArgumentTypeMismatchException(
		MethodArgumentTypeMismatchException exception
	) {
		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(RsData.of(HttpStatus.BAD_REQUEST, "요청 값의 형식이 올바르지 않습니다.", null));
	}
}
