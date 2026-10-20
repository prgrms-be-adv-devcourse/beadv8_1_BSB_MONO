package com.bukang.payout.out;

/**
 * 지급 배치가 예치금을 가진 서비스(캐시 서비스)에 정산금 이동을 요청하는 계약 (포트, PRO-252)
 * 지금은 동기 HTTP 어댑터가 구현한다. Kafka · 트랜잭션 아웃박스로 바꾸는 일은 PRO-253에서 다룬다
 */
@FunctionalInterface
public interface PayoutTransferPort {
	/**
	 * 정산금을 옮긴다. 같은 정산 ID로 다시 불러도 캐시 서비스는 돈을 한 번만 옮기고 성공으로 답한다
	 *
	 * @throws PayoutTransferRejectedException 캐시 서비스가 거절해 돈이 옮겨지지 않은 것이 확실할 때 (4xx)
	 * @throws PayoutTransferUnknownException  응답을 못 받았거나 서버 오류라 돈이 옮겨졌는지 모를 때
	 */
	void transfer(PayoutTransfer transfer);
}
