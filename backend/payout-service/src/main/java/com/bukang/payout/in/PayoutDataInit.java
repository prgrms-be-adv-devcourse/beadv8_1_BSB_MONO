package com.bukang.payout.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.payout.domain.BusinessDayCalendar;
import com.bukang.payout.domain.Payout;
import com.bukang.payout.domain.PayoutHold;
import com.bukang.payout.domain.PayoutHoldReason;
import com.bukang.payout.domain.PayoutTargetDate;
import com.bukang.payout.out.PayoutHoldRepository;
import com.bukang.payout.out.PayoutRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 개발용 판매자 정산 샘플 데이터 (판매자 화면 https://crewrun-alpha.vercel.app/seller 기준)
 * 대회 이름 · 참가비 · 판매 수는 화면에서 가져오고, 금액 · 지급일 · 정산 ID는 도메인 메서드가 세미 규칙(대회 다음 날 정산 1번)으로 계산한다.
 * 화면 숫자(1차 정산 40% 등)는 2단계 정산 기준이라 그대로 나오지 않는다.
 * 2026-10-12 기준 시나리오: 지급 완료 2건, 지급 예정 2건(10/20, 11/20), 지급보류 1건, 해제된 보류 1건.
 * 대회 서비스가 없어 raceId만 저장한다. 대회 샘플 데이터를 만들 때 아래 번호를 같은 대회에 쓴다.
 *   1 2026 봄 한강 10K / 2 2026 용감한 쿠키RUN in 서울 / 3 2026 춘천 나이트런 / 4 2027 부산 바다 10K / 5 2026 가을 트레일런
 *   6 2026 여름 야간 레이스 / 7 2026 한강 나이트런 / 8 2026 가을 하프마라톤 / 9 2026 서울숲 가을 10K
 *   (2 ~ 5는 대회 전이라 정산 대상일이 오지 않아 정산 데이터가 없다)
 */
@Profile("dev")
@Configuration
@Slf4j
public class PayoutDataInit {
	// member-service MemberDataInit의 가입 순서(system, holding, admin, user1~3, seller1)대로라면 seller1은 7, admin은 3이다
	// dev DB에 다른 회원이 먼저 있었다면 번호가 달라지므로 MEMBER_MEMBER에서 확인하고 맞춘다
	private static final int SELLER_ID = 7;
	private static final int ADMIN_ID = 3;

	private static final BigDecimal FEE_RATE = new BigDecimal("0.0550");
	private static final long PRICE_10K = 45_000L;
	private static final long PRICE_5K = 35_000L;
	private static final long PRICE_HALF = 60_000L;

	private final PayoutDataInit self;
	private final PayoutRepository payoutRepository;
	private final PayoutHoldRepository payoutHoldRepository;
	private final BusinessDayCalendar calendar;

	public PayoutDataInit(
		@Lazy PayoutDataInit self,
		PayoutRepository payoutRepository,
		PayoutHoldRepository payoutHoldRepository,
		BusinessDayCalendar calendar
	) {
		this.self = self;
		this.payoutRepository = payoutRepository;
		this.payoutHoldRepository = payoutHoldRepository;
		this.calendar = calendar;
	}

	@Bean
	@Order(1)
	public ApplicationRunner payoutDataInitApplicationRunner() {
		return args -> self.makeSamplePayouts();
	}

	@Transactional
	public void makeSamplePayouts() {
		// dev DB는 재시작해도 데이터가 남으므로(ddl-auto: update) 이미 만들었으면 건너뛴다
		if (payoutRepository.existsBySellerIdAndRaceIdAndPayoutMonth(SELLER_ID, 1, "2026-04")) {
			return;
		}

		// 봄 한강 10K (화면 '종료', 2,000 / 2,000): 4/13 대상일 → 5/20 지급 완료
		Payout spring = createPayout(1, LocalDate.of(2026, 4, 12), 2_000, PRICE_10K, PRICE_10K);
		spring.pay(LocalDateTime.of(2026, 5, 20, 4, 0));

		// 여름 야간 레이스: 판매자가 계좌를 늦게 등록한 경우. 대상일에 자동 보류 → 8/24 관리자 해제
		// → 해제일 뒤 지급일 9/20(일)이 밀려 9/21(월) 지급 완료
		Payout summer = createPayout(6, LocalDate.of(2026, 7, 18), 800, PRICE_10K, PRICE_5K);
		PayoutHold accountHold = hold(summer, PayoutHoldReason.ACCOUNT_NOT_REGISTERED, null,
			PayoutHold.SYSTEM_MEMBER_ID, LocalDateTime.of(2026, 7, 19, 3, 0));
		summer.release(accountHold, ADMIN_ID, LocalDateTime.of(2026, 8, 24, 10, 30), calendar);
		summer.pay(LocalDateTime.of(2026, 9, 21, 4, 0));

		// 한강 나이트런 (화면 정산 탭): 9/13 대상일 → 10/20 지급 예정. 요약의 정산 예정액이 된다
		createPayout(7, LocalDate.of(2026, 9, 12), 1_200, PRICE_10K, PRICE_5K);

		// 가을 하프마라톤: 9/27 대상일, 10/20 지급 예정이었으나 관리자가 10/5에 보류 (지급보류 1건)
		Payout half = createPayout(8, LocalDate.of(2026, 9, 26), 1_500, PRICE_HALF, PRICE_10K);
		hold(half, PayoutHoldReason.RACE_CANCELED, "기상 악화로 하프 종목 중단, 환불 범위 확인 중", ADMIN_ID,
			LocalDateTime.of(2026, 10, 5, 14, 0));

		// 서울숲 가을 10K: 10/4 대상일 → 11/20 지급 예정. 10/20 지급 뒤의 다음 정산일이 된다
		createPayout(9, LocalDate.of(2026, 10, 3), 600, PRICE_10K, PRICE_10K);

		log.info("정산 샘플 데이터 생성: sellerId={}", SELLER_ID);
	}

	// 정산 생성 배치(PayoutCreateUseCase)처럼 정산 대상일 03:00에 정산 내역과 항목을 만든다
	// 참가권은 10장 중 6장을 mainPrice, 4장을 subPrice로 섞는다 (예: 10K · 5K 종목)
	private Payout createPayout(int raceId, LocalDate raceDate, int count, long mainPrice, long subPrice) {
		LocalDate targetDate = PayoutTargetDate.of(raceDate);
		Payout payout = new Payout(SELLER_ID, raceId, YearMonth.from(targetDate), targetDate.atTime(3, 0), calendar);
		for (int i = 1; i <= count; i++) {
			long price = i % 10 < 6 ? mainPrice : subPrice;
			// 주문 상품 ID는 대회마다 겹치지 않게 대회 번호 × 100,000부터 붙인다
			payout.addItem(raceId * 100_000 + i, price, FEE_RATE, targetDate);
		}
		payoutRepository.save(payout);
		payout.assignPayoutCode();
		return payout;
	}

	private PayoutHold hold(Payout payout, PayoutHoldReason reasonType, String reason, int heldBy,
		LocalDateTime heldAt) {
		PayoutHold hold = payout.hold(reasonType, reason, heldBy, heldAt);
		payoutHoldRepository.save(hold);
		hold.assignHoldCode();
		return hold;
	}
}
