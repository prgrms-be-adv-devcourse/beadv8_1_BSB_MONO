package com.bukang.payout.app;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.bukang.payout.dto.RefundEligibilityDto;
import com.bukang.payout.out.RacePort;

import lombok.RequiredArgsConstructor;

/**
 * 참가권 환불 가능 여부 (PRO-28, PRO-32). 주문 서비스가 환불 요청을 받을 때 물어본다
 * 대회 ID를 받으면 상품 서비스에서 환불 마감일을 찾아 마감일 기준으로, 못 찾거나 대회 ID가 없으면 대회일 기준으로 판단한다
 */
@Service
@RequiredArgsConstructor
public class RefundEligibilityUseCase {
	private final RacePort racePort;

	public RefundEligibilityDto check(Integer raceId, LocalDate raceDate, LocalDate today) {
		Optional<LocalDate> refundDeadline = raceId == null ? Optional.empty() : racePort.findRefundDeadline(raceId);
		return RefundEligibilityDto.of(raceDate, refundDeadline, today);
	}
}
