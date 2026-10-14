package com.bukang.common.global.rsdata;

import org.springframework.http.HttpStatus;

import com.bukang.common.standard.resulttype.ResultType;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 성공·실패 공통 응답
 * code는 프론트가 오류 원인을 구분하는 값(예: MEMBER_DUPLICATE_EMAIL)이다. 코드가 없는 응답은 JSON에서 code 필드를 뺀다
 */
public record RsData<T>(
	int status,
	@JsonInclude(JsonInclude.Include.NON_NULL) String code,
	String message,
	T data
) implements ResultType<T> {
	public static <T> RsData<T> of(HttpStatus status, String message, T data) {
		return new RsData<>(status.value(), null, message, data);
	}

	public static <T> RsData<T> of(HttpStatus status, String code, String message, T data) {
		return new RsData<>(status.value(), code, message, data);
	}
}
