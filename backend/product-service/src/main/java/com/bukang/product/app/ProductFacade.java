package com.bukang.product.app;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.common.shared.member.event.MemberJoinedEvent;
import com.bukang.common.shared.seller.event.SellerChangedEvent;
import com.bukang.product.domain.ProductMember;
import com.bukang.product.domain.ProductSeller;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductFacade {
	private final ProductSyncMemberUseCase productSyncMemberUseCase;
	private final ProductSyncSellerUseCase productSyncSellerUseCase;

	@Transactional
	public ProductMember syncMember(MemberJoinedEvent event) {
		return productSyncMemberUseCase.syncMember(event);
	}

	@Transactional
	public Optional<ProductSeller> syncSeller(SellerChangedEvent event) {
		return productSyncSellerUseCase.syncSeller(event);
	}
}
