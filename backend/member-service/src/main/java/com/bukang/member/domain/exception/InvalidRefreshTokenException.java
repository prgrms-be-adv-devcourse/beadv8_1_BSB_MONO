package com.bukang.member.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

/**
 * Refresh Token으로 재발급할 수 없음 (쿠키 없음, 만료, 위조, 로그아웃·다른 기기 로그인으로 Redis 값과 다름).
 * 클라이언트는 401을 받으면 다시 로그인한다.
 */
public class InvalidRefreshTokenException extends BusinessException {
	public InvalidRefreshTokenException(String message) {
		super(HttpStatus.UNAUTHORIZED, message);
	}
}
