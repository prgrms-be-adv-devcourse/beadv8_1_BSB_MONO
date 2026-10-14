package com.bukang.order.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 정산용 판매 내역 조회 기간이 잘못됨 (시작일이 종료일보다 늦거나, 한 번에 조회할 수 있는 기간을 넘음)
public class SoldEntryPeriodException extends BusinessException {
	public SoldEntryPeriodException(String message) {
		super(HttpStatus.BAD_REQUEST, message);
	}
}
