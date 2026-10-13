package com.bukang.product.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;
import com.bukang.product.domain.HoldStatus;

/**
 * 해제·환불 복구로 이미 끝난 참가 신청으로 다시 자리를 확보하려 할 때 (참가 신청 1건 = 자리 1석, 다시 쓰지 않는다)
 */
public class HoldFinishedException extends BusinessException {
	public HoldFinishedException(HoldStatus status) {
		super(HttpStatus.CONFLICT, "이미 끝난 참가 신청이라 자리를 다시 확보할 수 없습니다. (자리 상태: %s)"
			.formatted(status.getLabel()));
	}
}
