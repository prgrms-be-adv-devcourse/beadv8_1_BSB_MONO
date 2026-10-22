package com.bukang.payout.app;

import org.springframework.stereotype.Service;

import com.bukang.payout.domain.exception.PayoutSellerMemberNotFoundException;
import com.bukang.payout.domain.exception.PayoutSellerNotApprovedException;
import com.bukang.payout.out.SellerLookupPort;
import com.bukang.payout.out.SellerLookupPort.SellerOfMember;

import lombok.RequiredArgsConstructor;

/**
 * 판매자 정산 조회에서 회원 ID(X-Member-Id)를 판매자 ID(MEMBER_SELLER.seller_id)로 바꾼다
 * 정산 내역은 판매자 ID로 묶여 있고 두 값은 다른 번호라, 회원 ID로 그대로 찾으면 남의 정산이 보이거나 내 정산이 안 보인다
 *
 * - 승인된 판매자: 그 판매자 ID
 * - 신청이 없거나 대기 · 반려: 403
 * - 존재하지 않거나 탈퇴한 회원: 404
 * - 회원 서비스에 묻지 못함: 503 (SellerLookupPort가 던진다)
 *
 * 다른 서비스를 부르므로 DB 트랜잭션 밖에서 부른다. 승인된 판매자가 둘 이상인 경우는 아직 다루지 않는다 (회원 서비스의 가장 최근 신청)
 */
@Service
@RequiredArgsConstructor
public class PayoutSellerIdResolver {
	private final SellerLookupPort sellerLookupPort;

	public int sellerIdOf(int memberId) {
		SellerOfMember seller = sellerLookupPort.findSellerOf(memberId)
			.orElseThrow(PayoutSellerMemberNotFoundException::new);
		if (!seller.isApproved()) {
			throw new PayoutSellerNotApprovedException();
		}
		return seller.sellerId();
	}
}
