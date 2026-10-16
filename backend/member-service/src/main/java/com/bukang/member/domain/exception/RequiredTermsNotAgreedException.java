package com.bukang.member.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

public class RequiredTermsNotAgreedException extends BusinessException {
	public RequiredTermsNotAgreedException(String message) {
		super(HttpStatus.BAD_REQUEST, "MEMBER_REQUIRED_TERMS_NOT_AGREED", message);
	}
}
