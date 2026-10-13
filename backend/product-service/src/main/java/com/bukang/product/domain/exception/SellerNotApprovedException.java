package com.bukang.product.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

/**
 * 승인된 판매자가 아닌 회원이 대회를 등록·수정하려 할 때 (판매자 복제본이 없거나 심사 중·반려)
 */
public class SellerNotApprovedException extends BusinessException {
	public SellerNotApprovedException() {
		super(HttpStatus.FORBIDDEN, "승인된 판매자만 대회를 등록·수정할 수 있습니다.");
	}
}
