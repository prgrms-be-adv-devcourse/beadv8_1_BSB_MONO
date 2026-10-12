package com.bukang.order.out.wallet;

/**
 * 예치금 서비스(D)에 결제(차감)와 환원을 요청한다. 잔액·원장·결제 기록(Payment)은 D 소유다 (D PRO-77~79)
 * 주문 ID를 멱등 키로 넘겨, 같은 주문의 재요청은 D가 한 번만 차감한다
 */
public interface WalletPort {
	WalletPayResult pay(int orderId, int memberId, int amount);

	// 차감한 뒤 자리 판매가 실패(기한 지남)하면 돌려준다. 같은 주문으로 여러 번 불러도 한 번만 돌려준다
	void cancelPayment(int orderId);
}
