package com.bukang.payout.out;

/**
 * 캐시 서비스가 정산금 이동 요청을 거절했다 (4xx). 돈이 옮겨지지 않은 것이 확실하므로 정산 예정으로 되돌린다 (PRO-252)
 * 다시 요청해도 같을 수 있지만(지갑 없음 · 계약 어긋남), 다음 실행에서 다시 시도하고 그때마다 error 로그를 남긴다
 */
public class PayoutTransferRejectedException extends RuntimeException {
	public PayoutTransferRejectedException(String message, Throwable cause) {
		super(message, cause);
	}
}
