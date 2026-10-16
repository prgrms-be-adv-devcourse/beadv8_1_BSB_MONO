package com.bukang.member.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

public class MemberNotAdminException extends BusinessException {
	public MemberNotAdminException(String message) {
		super(HttpStatus.FORBIDDEN, "MEMBER_NOT_ADMIN", message);
	}
}
