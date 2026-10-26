package com.bukang.payout.app;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.payout.domain.Payout;
import com.bukang.payout.domain.PayoutAccount;
import com.bukang.payout.domain.PayoutItem;
import com.bukang.payout.domain.PayoutRecordType;
import com.bukang.payout.domain.PayoutRecovery;
import com.bukang.payout.domain.PayoutRecoveryStatus;
import com.bukang.payout.domain.PayoutStage;
import com.bukang.payout.domain.PayoutStatus;
import com.bukang.payout.domain.TwoStagePayoutCalculator;
import com.bukang.payout.domain.exception.PayoutRecoveryConflictException;
import com.bukang.payout.out.PayoutItemRepository;
import com.bukang.payout.out.PayoutRecoveryRepository;
import com.bukang.payout.out.PayoutRepository;

import lombok.RequiredArgsConstructor;

/**
 * 원장 잔고와 회수 상계 (PRO-214)
 * 잔고는 판매자 × 대회 × 계정의 원장 줄을 더해 계산한다. 따로 저장하지 않는다
 * 상계는 갚을 돈(RECEIVABLE)을 같은 판매자 × 대회의 지급 전 정산 내역에서 바로 뺀다. 정산 내역 금액까지만 뺀다
 * 회수 요청 · 최종 생성 · 대회 취소가 같은 규칙을 쓰도록 한곳에 둔다
 * 일정 변경 환불의 회수 요청도 여기서 받는다 (payout.md 4-2, PRO-33)
 */
@Service
@RequiredArgsConstructor
public class PayoutLedgerUseCase {
	// 아직 지급하지 않은 정산 내역. 보류(HELD)도 지급 전이라 상계 대상이다
	private static final List<PayoutStatus> UNSENT = List.of(PayoutStatus.CALCULATED, PayoutStatus.HELD);

	private final PayoutRepository payoutRepository;
	private final PayoutItemRepository payoutItemRepository;
	private final PayoutRecoveryRepository payoutRecoveryRepository;

	/**
	 * 일정 변경 환불의 회수 요청 (payout.md 4-2, PRO-33 · PRO-214)
	 * 1차 정산을 만든 뒤의 환불은 1차 지급 전 · 뒤를 나누지 않고 항상 회수 요청으로 기록한다
	 * 회수액은 그 참가권의 1차 몫(가격 × 40%, 원 미만 버림)이다. 가격은 주문 쪽 값을 받는다
	 * 요청하면 같은 판매자 × 대회의 지급 전 정산 내역에서 바로 상계하고, 다 갚으면 회수가 바로 끝난다(구매자 환불로 이어짐)
	 *
	 * 지금은 정산 쪽 진입점만 만든다. 주문 서비스가 환불 신청 때 부르는 연결과 "회수 대기" 환불은 order 담당과 맞춘다
	 *
	 * @param raceCourseId 환불된 참가권의 코스 (원장 줄에 적어 코스별로 본다)
	 * @param price 환불된 참가권의 가격 (주문 상품 가격)
	 * @return 만든 회수. 1차 정산이 없으면(1차 정산금을 준 적이 없으면) 돌려받을 것이 없어 빈 값
	 */
	@Transactional
	public Optional<PayoutRecovery> recoverScheduleChange(int sellerId, int raceId, int raceCourseId,
		int orderItemId, long price, LocalDateTime now) {
		if (payoutRecoveryRepository.existsByOrderItemId(orderItemId)) {
			throw new PayoutRecoveryConflictException("이미 회수를 요청한 참가권입니다.");
		}
		if (findLive(sellerId, raceId, PayoutStage.FINAL).isPresent()) {
			// 최종 정산을 만든 뒤의 환불은 번 돈(EARNING)까지 되돌려야 해서 아직 정하지 않았다 (PRO-214 미룸)
			throw new PayoutRecoveryConflictException("최종 정산을 만든 뒤의 환불은 아직 회수할 수 없습니다.");
		}
		if (findLive(sellerId, raceId, PayoutStage.FIRST).isEmpty()) {
			return Optional.empty();
		}

		long amount = TwoStagePayoutCalculator.firstShare(price);
		PayoutRecovery recovery = openRecovery(
			PayoutRecovery.scheduleChanged(sellerId, raceId, orderItemId, amount, now), orderItemId, raceCourseId);
		settle(sellerId, raceId, now);
		return Optional.of(recovery);
	}

	// JPQL 합계 쿼리 전에 Hibernate가 쌓인 원장 줄을 먼저 내보내므로(자동 flush) 같은 트랜잭션에서 쓴 줄도 더해진다
	public long balance(int sellerId, int raceId, PayoutAccount account) {
		return payoutItemRepository.sumBalance(sellerId, raceId, account);
	}

	/**
	 * 갚을 돈을 지급 전 정산 내역에서 빼고, 다 갚았으면 그 대회의 요청 중인 회수를 상계로 완료한다
	 * 지급 전 정산 내역을 잠가서 읽으므로 지급 배치 · 다른 회수 요청과 정산 내역 금액을 동시에 바꾸지 않는다
	 * 다시 불러도 잔고로 다시 계산하므로 이미 뺀 금액을 또 빼지 않는다
	 *
	 * @return 다 갚아서 이번에 완료한 회수 (못 다 갚았으면 빈 목록. 남은 갚을 돈은 입금으로 받는다)
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public List<PayoutRecovery> settle(int sellerId, int raceId, LocalDateTime now) {
		long receivable = balance(sellerId, raceId, PayoutAccount.RECEIVABLE);
		if (receivable <= 0) {
			return List.of();
		}
		for (Payout payout : payoutRepository.findForUpdate(sellerId, raceId, UNSENT)) {
			receivable -= payout.offsetReceivable(receivable);
			if (receivable == 0) {
				break;
			}
		}
		if (receivable > 0) {
			return List.of();
		}
		List<PayoutRecovery> requested = payoutRecoveryRepository.findBySellerIdAndRaceIdAndStatus(
			sellerId, raceId, PayoutRecoveryStatus.REQUESTED);
		requested.forEach(recovery -> recovery.completeByOffset(now));
		return requested;
	}

	// 회수를 저장하고, 1차 정산금에서 빼서 같은 금액을 갚을 돈으로 옮긴다
	private PayoutRecovery openRecovery(PayoutRecovery recovery, Integer orderItemId, Integer raceCourseId) {
		PayoutRecovery saved = payoutRecoveryRepository.save(recovery);
		long amount = saved.getAmount();
		payoutItemRepository.save(
			PayoutItem.ofRecovery(saved, PayoutRecordType.RECOVERY_REQUESTED, orderItemId, raceCourseId, -amount));
		payoutItemRepository.save(
			PayoutItem.ofRecovery(saved, PayoutRecordType.RECEIVABLE_ADDED, orderItemId, raceCourseId, amount));
		return saved;
	}

	// 대회 취소로 닫히지 않은 그 단계의 정산 내역
	private Optional<Payout> findLive(int sellerId, int raceId, PayoutStage stage) {
		return payoutRepository.findFirstBySellerIdAndRaceIdAndPayoutStageOrderByIdAsc(sellerId, raceId, stage)
			.filter(payout -> payout.getStatus() != PayoutStatus.CANCELED);
	}
}
