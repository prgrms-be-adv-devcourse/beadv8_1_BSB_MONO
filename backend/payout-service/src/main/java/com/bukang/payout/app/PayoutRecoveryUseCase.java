package com.bukang.payout.app;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.payout.dto.PayoutRecoveryDto;

import lombok.RequiredArgsConstructor;

/**
 * 관리자의 회수 완료 (payout.md 4-2, PRO-33)
 * 입금 확인 규칙은 원장 유스케이스(PayoutLedgerUseCase.confirmDeposit)가 맡고, 여기서는 응답 DTO로 바꾼다
 */
@Service
@RequiredArgsConstructor
public class PayoutRecoveryUseCase {
	private final PayoutLedgerUseCase payoutLedgerUseCase;

	@Transactional
	public PayoutRecoveryDto complete(int recoveryId, int completedBy, LocalDateTime now) {
		return PayoutRecoveryDto.from(payoutLedgerUseCase.confirmDeposit(recoveryId, completedBy, now));
	}
}
