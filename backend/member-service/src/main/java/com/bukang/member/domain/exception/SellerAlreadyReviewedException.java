package com.bukang.member.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

public class SellerAlreadyReviewedException extends BusinessException {
	public SellerAlreadyReviewedException(String message) {
		super(HttpStatus.CONFLICT, "SELLER_ALREADY_REVIEWED", message);
	}
}
