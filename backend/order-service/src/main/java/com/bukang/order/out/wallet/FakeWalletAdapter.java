package com.bukang.order.out.wallet;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 임시 어댑터: 예치금 서비스(D)와 아직 연결하지 않아, 잔액이 늘 충분한 것처럼 결제를 성공시킨다
 * 예치금 서비스와 연결할 때(HTTP 호출, PRO-99) 이 클래스를 진짜 어댑터로 바꾼다
 */
@Slf4j
@Component
public class FakeWalletAdapter implements WalletPort {
	// 호출마다 경고를 남기면 로그가 묻히므로, 앱을 켤 때 한 번만 알린다
	public FakeWalletAdapter() {
		log.warn("예치금 서비스 연결 전이라 결제는 가짜 응답(FakeWalletAdapter)을 씁니다.");
	}

	@Override
	public WalletPayResult pay(int orderId, int memberId, int amount) {
		log.debug("가짜 예치금 결제 orderId={}, memberId={}, amount={}", orderId, memberId, amount);
		return WalletPayResult.paid();
	}

	@Override
	public void cancelPayment(int orderId) {
		log.debug("가짜 예치금 환원 orderId={}", orderId);
	}

	@Override
	public void refund(int orderId, int orderItemId, int amount) {
		log.debug("가짜 예치금 환불 orderId={}, orderItemId={}, amount={}", orderId, orderItemId, amount);
	}
}
