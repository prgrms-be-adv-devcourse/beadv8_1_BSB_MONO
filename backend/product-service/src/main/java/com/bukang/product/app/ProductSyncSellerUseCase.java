package com.bukang.product.app;

import org.springframework.stereotype.Service;

import com.bukang.common.shared.seller.event.SellerChangedEvent;
import com.bukang.product.domain.ProductMember;
import com.bukang.product.domain.ProductSeller;
import com.bukang.product.out.ProductMemberRepository;
import com.bukang.product.out.ProductSellerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductSyncSellerUseCase {
	private final ProductSellerRepository productSellerRepository;
	private final ProductMemberRepository productMemberRepository;

	// 원본 값으로 덮어쓴다 (등록 신청, 승인·반려 모두 같은 방식). 같은 메시지가 다시 와도 결과가 같다
	public ProductSeller syncSeller(SellerChangedEvent event) {
		// 회원은 조회하지 않고 참조만 건다. 회원 복제본이 아직 없으면 저장할 때 외래 키 오류가 난다
		ProductMember member = productMemberRepository.getReferenceById(event.memberId());
		return productSellerRepository.save(
			new ProductSeller(
				event.id(),
				member,
				event.sellerType(),
				event.businessName(),
				event.businessNumber(),
				event.representativeName(),
				event.status(),
				event.requestedAt(),
				event.reviewedAt()
			)
		);
	}
}
