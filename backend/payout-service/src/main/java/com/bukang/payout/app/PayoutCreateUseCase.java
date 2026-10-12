package com.bukang.payout.app;

import static java.util.stream.Collectors.groupingBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.payout.domain.BusinessDayCalendar;
import com.bukang.payout.domain.Payout;
import com.bukang.payout.domain.PayoutHold;
import com.bukang.payout.domain.PayoutHoldReason;
import com.bukang.payout.domain.PayoutTargetDate;
import com.bukang.payout.out.PayoutHoldRepository;
import com.bukang.payout.out.PayoutRepository;
import com.bukang.payout.out.PayoutSourcePort;
import com.bukang.payout.out.RacePort;
import com.bukang.payout.out.SellerAccountPort;
import com.bukang.payout.out.SoldEntry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 정산 대상일 배치: 오늘이 정산 대상일인 대회의 정산 내역(판매자 × 대회 × 월)과 정산 항목을 만든다 (payout.md 2-3, 2-6)
 * 판매자가 정산 계좌를 등록하지 않았으면 만들 때 '정산 계좌 미등록'으로 보류한다 (payout.md 1장 지급보류 2번, PRO-27)
 * 대회 이름은 이때 상품 서비스에 물어 정산 내역에 저장한다 (판매자 조회 때 상품 서비스를 부르지 않는다)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutCreateUseCase {
	// 플랫폼 수수료율 5.5% (부가세 포함). 정산 항목마다 기록 시점 값으로 남긴다
	private static final BigDecimal FEE_RATE = new BigDecimal("0.0550");
	// 자동 보류의 사유 상세. 관리자가 건 보류와 구분되도록 고정 문구를 남긴다
	private static final String AUTO_HOLD_REASON = "정산 계좌 미등록으로 자동 보류";
	private static final String UNKNOWN_RACE_NAME = "알 수 없는 대회 (ID %d)";

	private final PayoutRepository payoutRepository;
	private final PayoutHoldRepository payoutHoldRepository;
	private final PayoutSourcePort payoutSourcePort;
	private final SellerAccountPort sellerAccountPort;
	private final RacePort racePort;
	private final BusinessDayCalendar businessDayCalendar;

	@Transactional
	public void run(LocalDate targetDate, LocalDateTime now) {
		LocalDate raceDate = PayoutTargetDate.raceDateFor(targetDate);
		YearMonth payoutMonth = YearMonth.from(targetDate);

		Map<List<Integer>, List<SoldEntry>> bySellerAndRace = payoutSourcePort.findSoldEntries(raceDate).stream()
			.collect(groupingBy(entry -> List.of(entry.sellerId(), entry.raceId())));

		bySellerAndRace.forEach((key, entries) -> {
			int sellerId = key.get(0);
			int raceId = key.get(1);

			// 같은 날 배치가 다시 돌아도 이미 만든 묶음은 건너뛴다 (중복 지급 방지, DB 유일 제약이 마지막 방어선)
			if (payoutRepository.existsBySellerIdAndRaceIdAndPayoutMonth(sellerId, raceId, payoutMonth.toString())) {
				return;
			}

			Payout payout = new Payout(sellerId, raceId, raceNameOf(raceId), payoutMonth, now, businessDayCalendar);
			entries.forEach(entry -> payout.addItem(entry.orderItemId(), entry.price(), FEE_RATE, targetDate));

			// 정산 ID의 끝 4자리는 PK에서 만들므로 저장해서 ID를 받은 뒤에 붙인다
			payoutRepository.save(payout);
			payout.assignPayoutCode();

			// 계좌가 없으면 지급 배치가 고르지 않도록 HELD로 둔다. 판매자가 계좌를 등록하면 관리자가 보류를 푼다
			// 보류는 정산 내역의 PK를 참조하므로 정산 내역을 저장한 뒤에 건다
			if (!sellerAccountPort.hasPayoutAccount(sellerId)) {
				holdForMissingAccount(payout, now);
			}
		});
	}

	// 이름은 보여 주기용이라, 상품 서비스에 대회가 없어도 정산(돈)은 막지 않는다. 대신 경고를 남겨 데이터를 맞추게 한다
	private String raceNameOf(int raceId) {
		return racePort.findRaceName(raceId).orElseGet(() -> {
			log.warn("상품 서비스에서 대회를 찾지 못해 임시 이름으로 정산 내역을 만듭니다. raceId={}", raceId);
			return UNKNOWN_RACE_NAME.formatted(raceId);
		});
	}

	private void holdForMissingAccount(Payout payout, LocalDateTime now) {
		PayoutHold hold = payout.hold(
			PayoutHoldReason.ACCOUNT_NOT_REGISTERED, AUTO_HOLD_REASON, PayoutHold.SYSTEM_MEMBER_ID, now);
		payoutHoldRepository.save(hold);
		hold.assignHoldCode();
	}
}
