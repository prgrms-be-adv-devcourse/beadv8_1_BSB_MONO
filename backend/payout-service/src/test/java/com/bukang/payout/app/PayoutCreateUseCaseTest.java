package com.bukang.payout.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import com.bukang.payout.domain.PayoutItem;
import com.bukang.payout.domain.PayoutStatus;
import com.bukang.payout.out.PayoutHoldRepository;
import com.bukang.payout.out.PayoutRepository;
import com.bukang.payout.out.PayoutSourcePort;
import com.bukang.payout.out.SellerAccountPort;
import com.bukang.payout.out.SoldEntry;

@DataJpaTest
@ActiveProfiles("test")
class PayoutCreateUseCaseTest {
	private static final LocalDate RACE_DATE = LocalDate.of(2026, 9, 25);
	private static final LocalDate TARGET_DATE = LocalDate.of(2026, 9, 26);
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 26, 3, 0);

	@Autowired
	private PayoutRepository payoutRepository;

	@Autowired
	private PayoutHoldRepository payoutHoldRepository;

	// 정산 대상일의 대회일(9/25)로 물어볼 때만 판매된 참가권을 돌려주는 가짜 포트
	private PayoutSourcePort sourceReturning(List<SoldEntry> entries) {
		return raceDate -> raceDate.equals(RACE_DATE) ? entries : List.of();
	}

	// 모든 판매자가 정산 계좌를 등록했다
	private PayoutCreateUseCase useCaseSelling(List<SoldEntry> entries) {
		return useCaseSelling(entries, sellerId -> true);
	}

	private PayoutCreateUseCase useCaseSelling(List<SoldEntry> entries, SellerAccountPort sellerAccountPort) {
		return new PayoutCreateUseCase(payoutRepository, payoutHoldRepository, sourceReturning(entries),
			sellerAccountPort, new BusinessDayCalendar());
	}

	private Payout payoutOf(int sellerId) {
		return payoutRepository.findAll().stream()
			.filter(p -> p.getSellerId() == sellerId)
			.findFirst()
			.orElseThrow();
	}

	@Test
	@DisplayName("정산 대상일에 판매자 × 대회별로 정산 내역을 만들고, 참가권마다 정산 항목을 만든다")
	void createsPayoutPerSellerAndRace() {
		PayoutCreateUseCase useCase = useCaseSelling(List.of(
			new SoldEntry(100, 1, 10, 50_000L),
			new SoldEntry(101, 1, 10, 50_000L),
			new SoldEntry(200, 2, 20, 30_000L)
		));

		useCase.run(TARGET_DATE, NOW);

		List<Payout> payouts = payoutRepository.findAll();
		assertThat(payouts).hasSize(2);

		Payout seller1 = payouts.stream().filter(p -> p.getSellerId() == 1).findFirst().orElseThrow();
		assertThat(seller1.getRaceId()).isEqualTo(10);
		assertThat(seller1.getPayoutMonth()).isEqualTo("2026-09");
		assertThat(seller1.getStatus()).isEqualTo(PayoutStatus.CALCULATED);
		assertThat(seller1.getScheduledPayDate()).isEqualTo(LocalDate.of(2026, 10, 20));
		assertThat(seller1.getPayoutAmount()).isEqualTo(94_500L);
		assertThat(seller1.getItems())
			.extracting(PayoutItem::getOrderItemId)
			.containsExactlyInAnyOrder(100, 101);
		assertThat(seller1.getItems())
			.extracting(PayoutItem::getTargetDate)
			.containsOnly(TARGET_DATE);
	}

	@Test
	@DisplayName("정산 ID는 'ST-만든 날-16진수 4자리' 형식이다")
	void assignsPayoutCode() {
		PayoutCreateUseCase useCase = useCaseSelling(List.of(
			new SoldEntry(100, 1, 10, 50_000L)
		));

		useCase.run(TARGET_DATE, NOW);

		Payout payout = payoutRepository.findAll().getFirst();
		assertThat(payout.getPayoutCode()).matches("ST-20260926-[0-9A-F]{4}");
	}

	@Test
	@DisplayName("같은 날 배치를 두 번 돌려도 정산 내역이 중복으로 만들어지지 않는다")
	void runTwiceDoesNotDuplicate() {
		PayoutCreateUseCase useCase = useCaseSelling(List.of(
			new SoldEntry(100, 1, 10, 50_000L)
		));

		useCase.run(TARGET_DATE, NOW);
		useCase.run(TARGET_DATE, NOW.plusHours(1));

		assertThat(payoutRepository.findAll()).hasSize(1);
	}

	@Test
	@DisplayName("정산 계좌가 없는 판매자의 정산 내역은 만들어질 때 HELD이고, '정산 계좌 미등록' 자동 보류가 걸린다")
	void holdsPayoutOfSellerWithoutAccount() {
		// 판매자 2만 계좌가 없다
		PayoutCreateUseCase useCase = useCaseSelling(List.of(
			new SoldEntry(100, 1, 10, 50_000L),
			new SoldEntry(200, 2, 20, 30_000L)
		), sellerId -> sellerId != 2);

		useCase.run(TARGET_DATE, NOW);

		assertThat(payoutOf(1).getStatus()).isEqualTo(PayoutStatus.CALCULATED);
		assertThat(payoutOf(2).getStatus()).isEqualTo(PayoutStatus.HELD);

		List<PayoutHold> holds = payoutHoldRepository.findAll();
		assertThat(holds).hasSize(1);
		PayoutHold hold = holds.getFirst();
		assertThat(hold.getPayout().getId()).isEqualTo(payoutOf(2).getId());
		assertThat(hold.getReasonType()).isEqualTo(PayoutHoldReason.ACCOUNT_NOT_REGISTERED);
		assertThat(hold.getReason()).isEqualTo("정산 계좌 미등록으로 자동 보류");
		assertThat(hold.getStatus()).isEqualTo(PayoutHoldStatus.HELD);
		assertThat(hold.getHeldBy()).isEqualTo(PayoutHold.SYSTEM_MEMBER_ID);
		assertThat(hold.getHeldAt()).isEqualTo(NOW);
		assertThat(hold.getHoldCode()).matches("HD-20260926-[0-9A-F]{4}");
	}
}
