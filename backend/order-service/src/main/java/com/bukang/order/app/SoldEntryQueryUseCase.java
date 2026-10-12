package com.bukang.order.app;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.order.dto.SoldEntryDto;
import com.bukang.order.out.OrderItemRepository;

import lombok.RequiredArgsConstructor;

/**
 * 정산 서비스(E)가 정산 대상일 배치에서 부르는 판매 내역 조회 (payout PayoutSourcePort.findSoldEntries)
 * 그날 열린 대회의 결제 완료·미취소 참가권을 돌려준다
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SoldEntryQueryUseCase {
	private final OrderItemRepository orderItemRepository;

	public List<SoldEntryDto> findSoldEntries(LocalDate raceDate) {
		return orderItemRepository.findSoldEntries(raceDate.atStartOfDay(), raceDate.plusDays(1).atStartOfDay());
	}
}
