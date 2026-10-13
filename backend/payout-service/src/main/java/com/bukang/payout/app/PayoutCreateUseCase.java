package com.bukang.payout.app;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.bukang.payout.app.PayoutGroupCreator.PayoutGroup;
import com.bukang.payout.domain.PayoutTargetDate;
import com.bukang.payout.out.PayoutRepository;
import com.bukang.payout.out.RacePort;
import com.bukang.payout.out.SellerAccountPort;
import com.bukang.payout.out.SoldEntry;
import com.bukang.payout.out.SoldEntryPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 정산 대상일 배치: 정산 대상일이 된 대회의 정산 내역(판매자 × 대회 × 월)과 정산 항목을 만든다 (payout.md 2-3, 2-6)
 * 판매자가 정산 계좌를 등록하지 않았으면 만들 때 '정산 계좌 미등록'으로 보류한다 (payout.md 1장 지급보류 2번, PRO-27)
 * 대회 이름은 이때 상품 서비스에 물어 정산 내역에 저장한다 (판매자 조회 때 상품 서비스를 부르지 않는다)
 *
 * 묶음(판매자 × 대회)마다 트랜잭션을 따로 연다 (PRO-60). 하루치를 트랜잭션 하나로 만들면 한 묶음의 실패로
 * 그날 모든 판매자의 정산 내역이 롤백되고, 배치는 지난 날을 다시 보지 않아 다시 만들어지지 않는다
 * 그래서 이 클래스에는 트랜잭션을 걸지 않고, 실패한 묶음은 로그를 남기고 다음 묶음을 처리한다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutCreateUseCase {
	private static final String UNKNOWN_RACE_NAME = "알 수 없는 대회 (ID %d)";
	// 되돌아보기 기간: 어제 대회부터 7일치. 일주일 안의 장애는 저절로 따라잡는다 (PRO-61)
	private static final int LOOKBACK_DAYS = 7;

	private final PayoutRepository payoutRepository;
	private final PayoutGroupCreator payoutGroupCreator;
	private final SoldEntryPort soldEntryPort;
	private final SellerAccountPort sellerAccountPort;
	private final RacePort racePort;

	/**
	 * today의 배치를 돌린다. 어제까지 LOOKBACK_DAYS일 동안 열린 대회를 모두 보고, 아직 없는 묶음만 만든다 (PRO-61)
	 * 03:00 실행을 놓쳐도(서버 중단, 배포, 실패한 묶음) 다음 실행들이 따라잡는다. @Scheduled는 놓친 실행을 다시 돌리지 않는다
	 */
	public void run(LocalDate today, LocalDateTime now) {
		LocalDate toRaceDate = PayoutTargetDate.raceDateFor(today);
		LocalDate fromRaceDate = toRaceDate.minusDays(LOOKBACK_DAYS - 1);

		Map<SellerRace, List<SoldEntry>> bySellerAndRace = soldEntryPort.findSoldEntries(fromRaceDate, toRaceDate)
			.stream()
			.collect(groupingBy(entry -> new SellerRace(entry.sellerId(), entry.raceId())));

		int created = 0;
		int skipped = 0;
		int failed = 0;
		for (Map.Entry<SellerRace, List<SoldEntry>> group : bySellerAndRace.entrySet()) {
			SellerRace key = group.getKey();
			List<SoldEntry> entries = group.getValue();

			// 이 묶음만 실패로 남기고 다음 묶음을 계속 만든다. 실패한 묶음의 정산 내역 · 항목 · 보류는 함께 롤백된다
			try {
				// 정산 대상일과 정산 월은 실행일이 아니라 대회일로 정한다. 실행일로 정하면 월말 대회를 다음 달 실행이
				// 다시 볼 때 정산 월이 달라져 아래 중복 확인을 통과하고, 같은 참가권이 두 번 지급된다
				LocalDate targetDate = PayoutTargetDate.of(raceDateOf(key, entries));
				YearMonth payoutMonth = YearMonth.from(targetDate);

				// 이미 만든 묶음은 건너뛴다 (되돌아보기로 매일 같은 대회를 다시 보므로 대부분 여기서 걸린다)
				// 확인과 저장 사이에 다른 인스턴스가 먼저 저장하면 DB 유일 제약이 막고, 실패로 남는다
				if (payoutRepository.existsBySellerIdAndRaceIdAndPayoutMonth(
					key.sellerId(), key.raceId(), payoutMonth.toString())) {
					skipped++;
					continue;
				}

				PayoutGroup payoutGroup = new PayoutGroup(key.sellerId(), key.raceId(), raceNameOf(key.raceId()),
					sellerAccountPort.hasPayoutAccount(key.sellerId()), entries);
				payoutGroupCreator.create(payoutGroup, targetDate, now);
				created++;
			} catch (RuntimeException e) {
				failed++;
				log.error("정산 내역을 만들지 못했습니다. sellerId={}, raceId={}, 참가권 {}장",
					key.sellerId(), key.raceId(), entries.size(), e);
			}
		}

		log.info("정산 대상일 배치 완료. 대회일 {} ~ {}, 생성 {}, 이미 있음 {}, 실패 {}",
			fromRaceDate, toRaceDate, created, skipped, failed);
	}

	// 한 대회는 대회일이 하나다. 참가권마다 다르면 주문 쪽 데이터가 어긋난 것이므로, 어느 날로 정산할지 고르지 않고 실패로 남긴다
	private LocalDate raceDateOf(SellerRace key, List<SoldEntry> entries) {
		Set<LocalDate> raceDates = entries.stream().map(SoldEntry::raceDate).collect(toSet());
		if (raceDates.size() != 1) {
			throw new IllegalStateException(
				"한 대회의 참가권 대회일이 서로 다릅니다. raceId=%d, raceDates=%s".formatted(key.raceId(), raceDates));
		}
		return raceDates.iterator().next();
	}

	// 이름은 보여 주기용이라, 상품 서비스에 대회가 없어도 정산(돈)은 막지 않는다. 대신 경고를 남겨 데이터를 맞추게 한다
	private String raceNameOf(int raceId) {
		return racePort.findRaceName(raceId).orElseGet(() -> {
			log.warn("상품 서비스에서 대회를 찾지 못해 임시 이름으로 정산 내역을 만듭니다. raceId={}", raceId);
			return UNKNOWN_RACE_NAME.formatted(raceId);
		});
	}

	// 정산 내역 하나로 묶이는 판매자 × 대회
	private record SellerRace(int sellerId, int raceId) {
	}
}
