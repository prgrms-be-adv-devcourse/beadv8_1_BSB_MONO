package com.bukang.payout.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bukang.common.shared.seller.domain.SellerStatus;
import com.bukang.payout.domain.exception.PayoutSellerLookupUnavailableException;
import com.bukang.payout.domain.exception.PayoutSellerMemberNotFoundException;
import com.bukang.payout.domain.exception.PayoutSellerNotApprovedException;
import com.bukang.payout.out.SellerLookupPort.SellerOfMember;

// 판매자 정산 조회의 회원 ID → 판매자 ID 변환. 회원 7의 판매자 ID를 3으로 두어 두 값이 다른 경우를 본다
class PayoutSellerIdResolverTest {
	private static final int MEMBER_ID = 7;
	private static final int SELLER_ID = 3;

	@Test
	@DisplayName("승인된 판매자면 회원 ID가 아니라 판매자 ID를 돌려준다")
	void approved() {
		PayoutSellerIdResolver resolver = resolverAnswering(new SellerOfMember(SELLER_ID, SellerStatus.APPROVED));

		assertThat(resolver.sellerIdOf(MEMBER_ID)).isEqualTo(SELLER_ID);
	}

	@Test
	@DisplayName("대기 중인 신청이면 403이다")
	void pendingIsForbidden() {
		PayoutSellerIdResolver resolver = resolverAnswering(new SellerOfMember(SELLER_ID, SellerStatus.PENDING));

		assertThatThrownBy(() -> resolver.sellerIdOf(MEMBER_ID)).isInstanceOf(PayoutSellerNotApprovedException.class);
	}

	@Test
	@DisplayName("반려된 신청이면 403이다")
	void rejectedIsForbidden() {
		PayoutSellerIdResolver resolver = resolverAnswering(new SellerOfMember(SELLER_ID, SellerStatus.REJECTED));

		assertThatThrownBy(() -> resolver.sellerIdOf(MEMBER_ID)).isInstanceOf(PayoutSellerNotApprovedException.class);
	}

	@Test
	@DisplayName("판매자 신청이 없으면 403이다")
	void noSellerIsForbidden() {
		PayoutSellerIdResolver resolver = resolverAnswering(new SellerOfMember(null, null));

		assertThatThrownBy(() -> resolver.sellerIdOf(MEMBER_ID)).isInstanceOf(PayoutSellerNotApprovedException.class);
	}

	@Test
	@DisplayName("존재하지 않거나 탈퇴한 회원이면 404다")
	void unknownMemberIsNotFound() {
		PayoutSellerIdResolver resolver = new PayoutSellerIdResolver(memberId -> Optional.empty());

		assertThatThrownBy(() -> resolver.sellerIdOf(MEMBER_ID))
			.isInstanceOf(PayoutSellerMemberNotFoundException.class);
	}

	@Test
	@DisplayName("회원 서비스에 묻지 못하면 503을 그대로 올린다 (빈 목록으로 바꾸지 않는다)")
	void unavailablePropagates() {
		PayoutSellerIdResolver resolver = new PayoutSellerIdResolver(memberId -> {
			throw new PayoutSellerLookupUnavailableException();
		});

		assertThatThrownBy(() -> resolver.sellerIdOf(MEMBER_ID))
			.isInstanceOf(PayoutSellerLookupUnavailableException.class);
	}

	private PayoutSellerIdResolver resolverAnswering(SellerOfMember seller) {
		return new PayoutSellerIdResolver(memberId -> memberId == MEMBER_ID ? Optional.of(seller) : Optional.empty());
	}
}
