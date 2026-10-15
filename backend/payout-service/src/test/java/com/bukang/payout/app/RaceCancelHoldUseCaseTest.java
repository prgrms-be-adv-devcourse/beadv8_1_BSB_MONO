package com.bukang.payout.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.bukang.payout.domain.BusinessDayCalendar;
import com.bukang.payout.domain.Payout;
import com.bukang.payout.domain.PayoutHold;
import com.bukang.payout.domain.PayoutHoldReason;
import com.bukang.payout.domain.PayoutHoldStatus;
import com.bukang.payout.domain.PayoutStatus;
import com.bukang.payout.out.PayoutHoldRepository;
import com.bukang.payout.out.PayoutRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest
@ActiveProfiles("test")
class RaceCancelHoldUseCaseTest {
	private static final int RACE_ID = 10;
	private static final int OTHER_RACE_ID = 20;
	private static final int ADMIN_ID = 99;
	private static final LocalDateTime CANCELED_AT = LocalDateTime.of(2026, 10, 15, 14, 0);

	@Autowired
	private PayoutRepository payoutRepository;

	@Autowired
	private PayoutHoldRepository payoutHoldRepository;

	@Autowired
	private EntityManager entityManager;

	private RaceCancelHoldUseCase useCase() {
		return new RaceCancelHoldUseCase(payoutRepository, payoutHoldRepository);
	}

	private PayoutHoldUseCase holdUseCase() {
		return new PayoutHoldUseCase(payoutRepository, payoutHoldRepository, new BusinessDayCalendar());
	}

	// 유일 조건 (seller_id, race_id, payout_month)에 걸리지 않도록 판매자를 달리해 만든다
	private Payout savePayout(int sellerId, int raceId) {
		return payoutRepository.save(new Payout(sellerId, raceId, "테스트 대회", YearMonth.of(2026, 9),
			LocalDateTime.of(2026, 9, 29, 3, 0), new BusinessDayCalendar()));
	}

	private Payout reload(Payout payout) {
		entityManager.flush();
		entityManager.clear();
		return payoutRepository.findById(payout.getId()).orElseThrow();
	}

	private List<PayoutHold> holdsOf(Payout payout) {
		entityManager.flush();
		entityManager.clear();
		return payoutHoldRepository.findAll().stream()
			.filter(hold -> hold.getPayout().getId() == payout.getId())
			.toList();
	}

	@Test
	@DisplayName("취소된 대회의 정산 예정 정산 내역에 '대회 취소 처리' 보류를 시스템 이름으로 건다")
	void holdsCalculatedPayout() {
		Payout payout = savePayout(1, RACE_ID);

		RaceCancelHoldUseCase.HoldResult result = useCase().holdUnpaidPayouts(RACE_ID, CANCELED_AT);

		assertThat(result).isEqualTo(new RaceCancelHoldUseCase.HoldResult(1, 1));
		assertThat(reload(payout).getStatus()).isEqualTo(PayoutStatus.HELD);
		List<PayoutHold> holds = holdsOf(payout);
		assertThat(holds).hasSize(1);
		PayoutHold hold = holds.getFirst();
		assertThat(hold.getReasonType()).isEqualTo(PayoutHoldReason.RACE_CANCELED);
		assertThat(hold.getStatus()).isEqualTo(PayoutHoldStatus.HELD);
		assertThat(hold.getHeldBy()).isEqualTo(PayoutHold.SYSTEM_MEMBER_ID);
		assertThat(hold.getHeldAt()).isEqualTo(CANCELED_AT);
		assertThat(hold.getHoldCode()).isEqualTo("HD-20261015-%04X".formatted(hold.getId() & 0xFFFF));
	}

	@Test
	@DisplayName("이미 다른 사유로 보류 중이거나 지급 완료된 정산 내역, 다른 대회의 정산 내역은 건드리지 않는다")
	void skipsHeldPaidAndOtherRace() {
		Payout calculated = savePayout(1, RACE_ID);
		Payout heldForAccount = savePayout(2, RACE_ID);
		holdUseCase().hold(heldForAccount.getId(), PayoutHoldReason.ACCOUNT_NOT_REGISTERED, null, ADMIN_ID,
			CANCELED_AT.minusDays(1));
		Payout paid = savePayout(3, RACE_ID);
		paid.pay(CANCELED_AT.minusDays(1));
		Payout otherRace = savePayout(1, OTHER_RACE_ID);

		RaceCancelHoldUseCase.HoldResult result = useCase().holdUnpaidPayouts(RACE_ID, CANCELED_AT);

		assertThat(result).isEqualTo(new RaceCancelHoldUseCase.HoldResult(3, 1));
		assertThat(reload(calculated).getStatus()).isEqualTo(PayoutStatus.HELD);
		assertThat(holdsOf(heldForAccount)).singleElement()
			.extracting(PayoutHold::getReasonType).isEqualTo(PayoutHoldReason.ACCOUNT_NOT_REGISTERED);
		assertThat(reload(paid).getStatus()).isEqualTo(PayoutStatus.PAID);
		assertThat(holdsOf(paid)).isEmpty();
		assertThat(reload(otherRace).getStatus()).isEqualTo(PayoutStatus.CALCULATED);
		assertThat(holdsOf(otherRace)).isEmpty();
	}

	@Test
	@DisplayName("같은 대회 취소 메시지가 다시 와도 보류를 한 번만 건다")
	void idempotentOnRedelivery() {
		Payout payout = savePayout(1, RACE_ID);
		useCase().holdUnpaidPayouts(RACE_ID, CANCELED_AT);
		entityManager.flush();
		entityManager.clear();

		RaceCancelHoldUseCase.HoldResult again = useCase().holdUnpaidPayouts(RACE_ID, CANCELED_AT.plusMinutes(1));

		assertThat(again).isEqualTo(new RaceCancelHoldUseCase.HoldResult(1, 0));
		assertThat(holdsOf(payout)).hasSize(1);
	}

	@Test
	@DisplayName("관리자가 푼 대회 취소 보류는 같은 메시지가 다시 와도 다시 걸지 않는다")
	void doesNotReholdAfterAdminRelease() {
		Payout payout = savePayout(1, RACE_ID);
		useCase().holdUnpaidPayouts(RACE_ID, CANCELED_AT);
		int holdId = holdsOf(payout).getFirst().getId();
		holdUseCase().release(holdId, ADMIN_ID, CANCELED_AT.plusDays(1));
		entityManager.flush();
		entityManager.clear();

		RaceCancelHoldUseCase.HoldResult again = useCase().holdUnpaidPayouts(RACE_ID, CANCELED_AT.plusDays(2));

		assertThat(again.held()).isZero();
		assertThat(reload(payout).getStatus()).isEqualTo(PayoutStatus.CALCULATED);
		assertThat(holdsOf(payout)).singleElement()
			.extracting(PayoutHold::getStatus).isEqualTo(PayoutHoldStatus.RELEASED);
	}

	@Test
	@DisplayName("정산 내역이 없는 대회는 아무것도 하지 않는다 (세미는 정산 대상일 전에 취소돼 보통 이 경우다)")
	void noPayouts() {
		RaceCancelHoldUseCase.HoldResult result = useCase().holdUnpaidPayouts(RACE_ID, CANCELED_AT);

		assertThat(result).isEqualTo(new RaceCancelHoldUseCase.HoldResult(0, 0));
		assertThat(payoutHoldRepository.count()).isZero();
	}
}
