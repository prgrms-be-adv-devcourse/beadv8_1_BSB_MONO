package com.bukang.payout.app;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.payout.domain.PayoutRecovery;
import com.bukang.payout.domain.exception.PayoutRecoveryNotFoundException;
import com.bukang.payout.dto.PayoutRecoveryDto;
import com.bukang.payout.out.PayoutRecoveryRepository;

import lombok.RequiredArgsConstructor;

/**
 * 관리자의 회수 완료: 판매자의 입금을 확인하고 회수를 끝낸다 (payout.md 4-2, PRO-33)
 * 상태 규칙은 PayoutRecovery가 지키고, 여기서는 잠가서 찾고 넘기기만 한다
 */
@Service
@RequiredArgsConstructor
public class PayoutRecoveryUseCase {
	private final PayoutRecoveryRepository payoutRecoveryRepository;

	@Transactional
	public PayoutRecoveryDto complete(int recoveryId, int completedBy, LocalDateTime now) {
		PayoutRecovery recovery = payoutRecoveryRepository.findByIdForUpdate(recoveryId)
			.orElseThrow(PayoutRecoveryNotFoundException::new);
		recovery.complete(completedBy, now);
		return PayoutRecoveryDto.from(recovery);
	}
}
