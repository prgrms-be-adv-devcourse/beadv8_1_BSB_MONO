package com.bukang.product.app;

import org.springframework.stereotype.Service;

import com.bukang.common.shared.member.event.MemberJoinedEvent;
import com.bukang.product.domain.ProductMember;
import com.bukang.product.out.ProductMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductSyncMemberUseCase {
	private final ProductMemberRepository productMemberRepository;

	// 원본 값으로 덮어쓴다 (ID가 있으면 UPDATE, 없으면 INSERT). 같은 메시지가 다시 와도 결과가 같다
	public ProductMember syncMember(MemberJoinedEvent event) {
		return productMemberRepository.save(
			new ProductMember(
				event.id(),
				event.createDate(),
				event.modifyDate(),
				event.nickname()
			)
		);
	}
}
