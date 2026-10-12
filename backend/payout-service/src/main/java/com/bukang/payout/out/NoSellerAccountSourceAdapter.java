package com.bukang.payout.out;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 임시 어댑터: 판매자 정산 계좌를 가진 서비스가 아직 없어 모든 판매자가 계좌를 등록했다고 돌려준다
 * '없음'으로 두면 주문 서비스가 먼저 연결될 때 모든 정산 내역이 보류되어 지급되지 않으므로 '있음'으로 둔다
 * 계좌를 가진 서비스와 연결할 때(HTTP 호출 또는 Kafka 복제본) 이 클래스를 진짜 어댑터로 바꾼다
 */
@Slf4j
@Component
public class NoSellerAccountSourceAdapter implements SellerAccountPort {
	@Override
	public boolean hasPayoutAccount(int sellerId) {
		log.warn("판매자 계좌 정보 연결 전이라 계좌가 있다고 봅니다. sellerId={}", sellerId);
		return true;
	}
}
