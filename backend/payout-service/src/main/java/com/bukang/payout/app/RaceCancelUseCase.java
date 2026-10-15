package com.bukang.payout.app;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.payout.domain.Payout;
import com.bukang.payout.domain.PayoutHold;
import com.bukang.payout.domain.PayoutRecovery;
import com.bukang.payout.domain.PayoutRecoveryReason;
import com.bukang.payout.domain.PayoutStatus;
import com.bukang.payout.out.PayoutRecoveryRepository;
import com.bukang.payout.out.PayoutRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 대회 취소 처리: 그 대회의 정산 내역을 지급 전후로 나눠 처리한다 (10/15 결정, payout.md 4-3 [제안])
 * - 지급 전(정산 예정 · 지급보류): 취소(CANCELED)로 닫는다. 보류로 막아 두면 일찍 풀었을 때 돈이 나가므로 닫는다 (PRO-34)
 * - 지급 완료: 지급액 합만큼 회수를 대회당 1건 만든다. 판매자 입금은 관리자가 확인한다 (PRO-33)
 * 구매자 환불은 주문 서비스가 같은 이벤트로 바로 하고, 회수를 기다리지 않는다
 * 세미는 정산 대상일(대회 다음 날) 전에 대회가 취소되므로 처리할 정산 내역이 없는 게 보통이다 (payout.md 2-5)
 * 2단계 정산(PRO-32)에서는 1차 정산이 지급 전이면 닫히고, 지급 뒤면 회수가 된다. 한 대회에 둘이 함께 생기지 않는다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RaceCancelUseCase {
	private static final String CANCEL_REASON = "대회 취소";

	private final PayoutRepository payoutRepository;
	private final PayoutRecoveryRepository payoutRecoveryRepository;

	// 한 트랜잭션으로 묶는다. 지급 배치와 부딪혀 낙관적 락으로 실패하면 모두 되돌리고, 메시지를 다시 처리한다
	// 다시 처리해도 이미 닫은 정산 내역은 CANCELED라 건너뛰고, 회수는 이미 있으면 만들지 않는다
	@Transactional
	public CancelResult cancelRace(int raceId, LocalDateTime now) {
		List<Payout> payouts = payoutRepository.findByRaceId(raceId);
		int canceled = 0;
		long paidAmount = 0;
		Integer sellerId = null;
		for (Payout payout : payouts) {
			if (payout.getStatus() == PayoutStatus.CALCULATED || payout.getStatus() == PayoutStatus.HELD) {
				payout.cancel(PayoutHold.SYSTEM_MEMBER_ID, now, CANCEL_REASON);
				canceled++;
			} else if (payout.getStatus() == PayoutStatus.PAID) {
				paidAmount += payout.getPayoutAmount();
				sellerId = payout.getSellerId();
			}
		}

		boolean recoveryCreated = false;
		// 지급액이 0원인 정산 내역(0원 지급 완료)만 있으면 돌려받을 돈이 없다
		if (paidAmount > 0
			&& !payoutRecoveryRepository.existsByRaceIdAndReasonType(raceId, PayoutRecoveryReason.RACE_CANCELED)) {
			payoutRecoveryRepository.save(
				new PayoutRecovery(sellerId, raceId, PayoutRecoveryReason.RACE_CANCELED, paidAmount, now));
			recoveryCreated = true;
		}
		log.info("대회 취소 처리: raceId={}, 정산 내역 {}건 중 {}건 취소, 지급 완료액 {}원, 회수 생성 {}",
			raceId, payouts.size(), canceled, paidAmount, recoveryCreated);
		return new CancelResult(payouts.size(), canceled, recoveryCreated);
	}

	/**
	 * @param found 그 대회의 정산 내역 수
	 * @param canceled 이번에 취소로 닫은 정산 내역 수
	 * @param recoveryCreated 이번에 회수를 만들었는지
	 */
	public record CancelResult(int found, int canceled, boolean recoveryCreated) {
	}
}
