package com.bukang.member.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

public class SellerNotFoundException extends BusinessException {
	public SellerNotFoundException(String message) {
		super(HttpStatus.NOT_FOUND, "SELLER_NOT_FOUND", message);
	}
}
