package com.bukang.member.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

public class SellerAlreadyAppliedException extends BusinessException {
	public SellerAlreadyAppliedException(String message) {
		super(HttpStatus.CONFLICT, "SELLER_ALREADY_APPLIED", message);
	}
}
