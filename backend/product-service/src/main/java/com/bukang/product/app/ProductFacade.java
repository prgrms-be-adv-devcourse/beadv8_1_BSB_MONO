package com.bukang.product.app;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.common.shared.member.event.MemberJoinedEvent;
import com.bukang.common.shared.seller.event.SellerChangedEvent;
import com.bukang.product.domain.ProductMember;
import com.bukang.product.domain.ProductSeller;
import com.bukang.product.domain.Race;
import com.bukang.product.domain.RaceCourse;
import com.bukang.product.dto.RaceSaveDto;
import com.bukang.product.dto.RaceSaveRequestDto;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductFacade {
	private final ProductSyncMemberUseCase productSyncMemberUseCase;
	private final ProductSyncSellerUseCase productSyncSellerUseCase;
	private final RaceRegisterUseCase raceRegisterUseCase;
	private final RaceUpdateUseCase raceUpdateUseCase;

	@Transactional
	public ProductMember syncMember(MemberJoinedEvent event) {
		return productSyncMemberUseCase.syncMember(event);
	}

	@Transactional
	public Optional<ProductSeller> syncSeller(SellerChangedEvent event) {
		return productSyncSellerUseCase.syncSeller(event);
	}

	@Transactional
	public RaceSaveDto registerRace(int memberId, RaceSaveRequestDto request) {
		return toSaveDto(raceRegisterUseCase.register(memberId, request));
	}

	@Transactional
	public RaceSaveDto updateRace(int memberId, int raceId, RaceSaveRequestDto request) {
		return toSaveDto(raceUpdateUseCase.update(memberId, raceId, request));
	}

	private RaceSaveDto toSaveDto(Race race) {
		return new RaceSaveDto(
			race.getId(),
			race.getSaleStatus(),
			race.getSaleOpenAt(),
			race.getCourses().stream().map(RaceCourse::getId).toList()
		);
	}
}
