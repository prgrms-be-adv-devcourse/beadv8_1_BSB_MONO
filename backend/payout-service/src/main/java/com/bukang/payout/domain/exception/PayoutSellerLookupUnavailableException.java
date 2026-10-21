package com.bukang.payout.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

// 회원 서비스에서 판매자 정보를 확인하지 못해 판매자 정산을 보여 줄 수 없다
// 메시지가 그대로 응답에 나가므로 원인(주소 · 예외)은 넣지 않고, 던지는 쪽이 로그로 남긴다
public class PayoutSellerLookupUnavailableException extends BusinessException {
	public PayoutSellerLookupUnavailableException() {
		super(HttpStatus.SERVICE_UNAVAILABLE, "판매자 정보를 확인하지 못했습니다. 잠시 뒤 다시 시도해주세요.");
	}
}
