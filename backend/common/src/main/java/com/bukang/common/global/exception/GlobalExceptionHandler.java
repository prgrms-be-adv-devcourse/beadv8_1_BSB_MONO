package com.bukang.common.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.bukang.common.global.rsdata.RsData;

/**
 * 모든 서비스가 공통으로 쓰는 예외 처리
 * 어느 핸들러도 처리하지 못한 예외(500)는 여기가 아니라 UnexpectedExceptionResolver가 맡는다
 * Spring Security 같은 특정 서비스 전용 예외는 각 서비스에서 따로 처리한다 (예: member-service의 AuthExceptionHandler)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
	// 입력값 오류(@Valid 검증 실패, 요청 본문 형식 오류)의 오류 코드
	private static final String INVALID_REQUEST = "INVALID_REQUEST";

	// 도메인 예외: 상태 코드는 각 예외 클래스가 정한다 (BusinessException 상속)
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<RsData<Void>> businessException(BusinessException exception) {
		return ResponseEntity
			.status(exception.getStatus())
			.body(RsData.of(exception.getStatus(), exception.getCode(), exception.getMessage(), null));
	}

	// @Valid 검증 실패: 첫 번째 필드 오류 메시지를 응답한다
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<RsData<Void>> methodArgumentNotValidException(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getFieldErrors().stream()
			.findFirst()
			.map(fieldError -> fieldError.getDefaultMessage())
			.orElse("요청 값이 올바르지 않습니다.");

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(RsData.of(HttpStatus.BAD_REQUEST, INVALID_REQUEST, message, null));
	}

	// 요청 본문(JSON) 형식 오류: 처리하지 않으면 /error로 넘어가 인증 오류(401)처럼 응답된다
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<RsData<Void>> httpMessageNotReadableException(HttpMessageNotReadableException exception) {
		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(RsData.of(HttpStatus.BAD_REQUEST, INVALID_REQUEST, "요청 본문 형식이 올바르지 않습니다.", null));
	}
}
