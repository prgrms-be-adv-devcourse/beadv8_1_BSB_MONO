package com.bukang.product.in;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bukang.product.app.ProductFacade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 5분마다 대회의 판매 상태를 바꾼다 (판매 시작일 0시부터 판매 중, 대회일 다음 날 0시부터 판매 종료)
 * 상태는 최대 5분 늦게 바뀐다. 접수할 수 있는지는 주문·상품 서비스가 접수 시각으로 따로 판단하므로 상태는 판매자 화면·검색용이다.
 * 테스트에서는 등록하지 않는다. 테스트는 RaceSaleStatusUseCase에 시각을 넣어 직접 부른다
 */
@Slf4j
@Profile("!test")
@Component
@RequiredArgsConstructor
public class RaceSaleStatusScheduler {
	private final ProductFacade productFacade;

	@Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
	public void refreshSaleStatuses() {
		int changed = productFacade.refreshSaleStatuses();
		if (changed > 0) {
			log.info("대회 판매 상태를 바꿨습니다. 대회 수={}", changed);
		}
	}
}
