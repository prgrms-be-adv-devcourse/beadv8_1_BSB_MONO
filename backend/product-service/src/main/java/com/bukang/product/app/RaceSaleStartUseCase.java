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
 * 판매 예정에서 판매 중으로는 판매 시작 일시가 지난 뒤 RaceSaleStatusScheduler가 5분마다 바꾼다.
 */
@Service
@RequiredArgsConstructor
public class RaceSaleStartUseCase {
	private final ProductSellerRepository productSellerRepository;
	private final RaceRepository raceRepository;

	public RaceSaveResult startSale(int memberId, int raceId) {
		ProductSeller seller = productSellerRepository.findByMemberIdAndStatus(memberId, SellerStatus.APPROVED)
			.orElseThrow(SellerNotApprovedException::new);
		// 잠그고 읽는다. 같은 순간 취소가 커밋되면 취소된 상태를 보고 막힌다 (잠그지 않으면 취소를 판매 예정으로 덮어쓴다)
		Race race = raceRepository.findByIdForUpdate(raceId).orElseThrow(RaceNotFoundException::new);
		if (!race.isOwnedBy(seller)) {
			throw new RaceAccessDeniedException();
		}
		race.startSale(LocalDateTime.now());

		// 검색 문서의 판매 상태만 다시 색인한다. 이미지는 바뀌지 않으므로 파일 연결 메시지는 없다(null)
		return RaceSaveResult.of(race, List.of(), null);
	}
}
