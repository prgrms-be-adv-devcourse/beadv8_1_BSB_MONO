package com.bukang.order.support;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.bukang.order.out.seller.SellerLookupPort;

/**
 * 테스트용 판매자 ID 조회. 테스트가 정한 회원만 승인된 판매자다 (가짜 어댑터처럼 누구나 판매자가 되지 않는다)
 */
public class StubSellerLookupPort implements SellerLookupPort {
	private final Map<Integer, Integer> sellerIdByMemberId = new ConcurrentHashMap<>();

	@Override
	public Optional<Integer> findApprovedSellerId(int memberId) {
		return Optional.ofNullable(sellerIdByMemberId.get(memberId));
	}

	public void approve(int memberId, int sellerId) {
		sellerIdByMemberId.put(memberId, sellerId);
	}

	public void clear() {
		sellerIdByMemberId.clear();
	}
}
