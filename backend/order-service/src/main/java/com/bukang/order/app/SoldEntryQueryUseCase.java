package com.bukang.order.app;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.order.domain.exception.SoldEntryPeriodException;
import com.bukang.order.dto.SoldEntryDto;
import com.bukang.order.out.OrderItemRepository;

import lombok.RequiredArgsConstructor;

/**
 * 정산 서비스(E)가 정산 대상일 배치에서 부르는 판매 내역 조회 (payout SoldEntryPort.findSoldEntries)
 * from ~ to(양 끝 포함)에 열린 대회의 결제 완료·미취소 참가권을 돌려준다
 * 정산 배치는 놓친 날을 따라잡으려고 어제부터 7일치를 한 번에 묻는다 (PRO-61)
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SoldEntryQueryUseCase {
	// 한 번에 조회할 수 있는 최대 일수. 정산 배치는 7일을 묻고, 잘못된 호출이 대량 조회가 되지 않게 막는다
	private static final int MAX_PERIOD_DAYS = 31;

	private final OrderItemRepository orderItemRepository;

	public List<SoldEntryDto> findSoldEntries(LocalDate from, LocalDate to) {
		if (from.isAfter(to)) {
			throw new SoldEntryPeriodException("조회 시작일이 종료일보다 늦습니다.");
		}
		if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_PERIOD_DAYS) {
			throw new SoldEntryPeriodException("판매 내역은 한 번에 %d일까지 조회할 수 있습니다.".formatted(MAX_PERIOD_DAYS));
		}
		return orderItemRepository.findSoldEntries(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
	}
}
