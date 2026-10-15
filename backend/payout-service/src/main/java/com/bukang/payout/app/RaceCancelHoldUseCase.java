package com.bukang.payout.app;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.payout.domain.Payout;
import com.bukang.payout.domain.PayoutHold;
import com.bukang.payout.domain.PayoutHoldReason;
import com.bukang.payout.domain.PayoutStatus;
import com.bukang.payout.out.PayoutHoldRepository;
import com.bukang.payout.out.PayoutRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 대회 취소 자동 보류: 대회가 취소되면 그 대회의 지급 전 정산 내역에 '대회 취소 처리' 보류를 건다 (PRO-34, payout.md 5-2)
 * 회수 · 환불이 끝나면 관리자가 기존 해제 API로 푼다
 * 세미는 정산 대상일(대회 다음 날) 전에 대회가 취소되므로 걸 정산 내역이 없는 게 보통이다 (payout.md 2-5)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RaceCancelHoldUseCase {
	private static final String AUTO_HOLD_REASON = "대회 취소로 자동 보류";

	private final PayoutRepository payoutRepository;
	private final PayoutHoldRepository payoutHoldRepository;

	// 한 트랜잭션으로 묶는다. 지급 배치와 부딪혀 한 건이라도 낙관적 락으로 실패하면 모두 되돌리고, 메시지를 다시 처리한다
	@Transactional
	public HoldResult holdUnpaidPayouts(int raceId, LocalDateTime now) {
		List<Payout> payouts = payoutRepository.findByRaceId(raceId);
		int held = 0;
		for (Payout payout : payouts) {
			if (!shouldHold(payout)) {
				continue;
			}
			PayoutHold hold = payout.hold(PayoutHoldReason.RACE_CANCELED, AUTO_HOLD_REASON,
				PayoutHold.SYSTEM_MEMBER_ID, now);
			// 지급보류 ID의 끝 4자리는 PK에서 만들므로 저장해서 ID를 받은 뒤에 붙인다
			payoutHoldRepository.save(hold);
			hold.assignHoldCode();
			held++;
		}
		log.info("대회 취소 자동 보류: raceId={}, 정산 내역 {}건 중 {}건 보류", raceId, payouts.size(), held);
		return new HoldResult(payouts.size(), held);
	}

	// 정산 예정(CALCULATED)만 건다. hold()는 HELD · PAID면 예외를 던지므로 그 전에 거른다
	// - HELD: 계좌 미등록 등 다른 사유로 이미 지급이 막혀 있다. 보류는 하나만 두므로 건너뛴다
	// - PAID: 이미 지급했다. 지급 뒤 대회 취소는 회수(PRO-33)가 맡는다
	// - 대회 취소 보류를 건 적이 있으면(해제 포함) 건너뛴다. 같은 이벤트가 다시 와도, 관리자가 푼 보류를 다시 걸지 않는다
	private boolean shouldHold(Payout payout) {
		if (payout.getStatus() != PayoutStatus.CALCULATED) {
			return false;
		}
		return !payoutHoldRepository.existsByPayoutIdAndReasonType(payout.getId(), PayoutHoldReason.RACE_CANCELED);
	}

	/**
	 * @param found 그 대회의 정산 내역 수
	 * @param held 이번에 보류를 건 정산 내역 수
	 */
	public record HoldResult(int found, int held) {
	}
}
