package com.bukang.member.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

public class SellerNotReviewedException extends BusinessException {
	public SellerNotReviewedException(String message) {
		super(HttpStatus.CONFLICT, "SELLER_NOT_REVIEWED", message);
	}
}
