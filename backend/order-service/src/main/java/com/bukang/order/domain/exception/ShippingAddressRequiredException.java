package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 키트가 있는 종목인데 배송 주소를 적지 않음
public class ShippingAddressRequiredException extends BusinessException {
	public static final String CODE = "ORDER_SHIPPING_ADDRESS_REQUIRED";

	public ShippingAddressRequiredException(String message) {
		super(HttpStatus.BAD_REQUEST, CODE, message);
	}
}
