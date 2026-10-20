package com.bukang.cash.app;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.bukang.cash.domain.member.CashMember;
import com.bukang.cash.domain.seller.CashSeller;
import com.bukang.cash.out.CashMemberRepository;
import com.bukang.cash.out.CashSellerRepository;
import com.bukang.common.shared.seller.event.SellerChangedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 판매자 변경 이벤트로 CashSeller(판매자 신청서 ID → 회원) 복제본을 만든다 (PRO-256)
 * product-service의 ProductSyncSellerUseCase와 같은 방식: 등록 신청·승인·반려 모두 원본 값으로 덮어쓴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CashSyncSellerUseCase {
	private final CashSellerRepository cashSellerRepository;
	private final CashMemberRepository cashMemberRepository;

	public Optional<CashSeller> syncSeller(SellerChangedEvent event) {
		// 회원가입을 해야 판매자 등록을 할 수 있으므로, 회원 복제본이 없는 판매자 메시지는 잘못된 메시지다
		// 다시 시도하지 않고 에러 로그만 남긴 뒤 건너뛴다
		Optional<CashMember> member = cashMemberRepository.findById(event.memberId());
		if (member.isEmpty()) {
			log.error("회원 복제본이 없는 판매자 메시지를 건너뜁니다. sellerId={}, memberId={}", event.id(), event.memberId());
			return Optional.empty();
		}
		return Optional.of(cashSellerRepository.save(new CashSeller(event.id(), member.get())));
	}
}
