package com.bukang.product.app;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.bukang.common.shared.seller.domain.SellerStatus;
import com.bukang.product.domain.ProductSeller;
import com.bukang.product.domain.Race;
import com.bukang.product.domain.exception.RaceAccessDeniedException;
import com.bukang.product.domain.exception.RaceNotFoundException;
import com.bukang.product.domain.exception.SellerNotApprovedException;
import com.bukang.product.out.ProductSellerRepository;
import com.bukang.product.out.RaceRepository;

import lombok.RequiredArgsConstructor;

/**
 * 판매자가 작성 중인 대회의 판매를 시작한다 (작성 중 → 판매 예정, 판매 시작 일시가 지났으면 판매 중)
 * 판매 예정에서 판매 중으로 바꾸는 방법(스케줄러 / 조회할 때 판정)은 조회·정원 작업에서 정한다.
 */
@Service
@RequiredArgsConstructor
public class RaceSaleStartUseCase {
	private final ProductSellerRepository productSellerRepository;
	private final RaceRepository raceRepository;

	public RaceSaveResult startSale(int memberId, int raceId) {
		ProductSeller seller = productSellerRepository.findByMemberIdAndStatus(memberId, SellerStatus.APPROVED)
			.orElseThrow(SellerNotApprovedException::new);
		Race race = raceRepository.findById(raceId).orElseThrow(RaceNotFoundException::new);
		if (!race.isOwnedBy(seller)) {
			throw new RaceAccessDeniedException();
		}
		race.startSale(LocalDateTime.now());

		// 검색 문서의 판매 상태만 다시 색인한다. 이미지는 바뀌지 않으므로 파일 연결 메시지는 없다(null)
		return RaceSaveResult.of(race, List.of(), null);
	}
}
