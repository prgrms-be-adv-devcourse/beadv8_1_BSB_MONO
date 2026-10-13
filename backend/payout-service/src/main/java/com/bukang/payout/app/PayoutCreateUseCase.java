package com.bukang.payout.app;

import static java.util.stream.Collectors.groupingBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

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
 * 정산 대상일 배치: 오늘이 정산 대상일인 대회의 정산 내역(판매자 × 대회 × 월)과 정산 항목을 만든다 (payout.md 2-3, 2-6)
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

	private final PayoutRepository payoutRepository;
	private final PayoutGroupCreator payoutGroupCreator;
	private final SoldEntryPort soldEntryPort;
	private final SellerAccountPort sellerAccountPort;
	private final RacePort racePort;

	public void run(LocalDate targetDate, LocalDateTime now) {
		LocalDate raceDate = PayoutTargetDate.raceDateFor(targetDate);
		YearMonth payoutMonth = YearMonth.from(targetDate);

		Map<List<Integer>, List<SoldEntry>> bySellerAndRace = soldEntryPort.findSoldEntries(raceDate).stream()
			.collect(groupingBy(entry -> List.of(entry.sellerId(), entry.raceId())));

		int created = 0;
		int skipped = 0;
		int failed = 0;
		for (Map.Entry<List<Integer>, List<SoldEntry>> group : bySellerAndRace.entrySet()) {
			int sellerId = group.getKey().get(0);
			int raceId = group.getKey().get(1);

			// 같은 날 배치가 다시 돌아도 이미 만든 묶음은 건너뛴다 (중복 지급 방지)
			// 확인과 저장 사이에 다른 인스턴스가 먼저 저장하면 DB 유일 제약이 막고, 아래에서 실패로 남는다
			if (payoutRepository.existsBySellerIdAndRaceIdAndPayoutMonth(sellerId, raceId, payoutMonth.toString())) {
				skipped++;
				continue;
			}

			// 이 묶음만 실패로 남기고 다음 묶음을 계속 만든다. 실패한 묶음의 정산 내역 · 항목 · 보류는 함께 롤백된다
			try {
				PayoutGroup payoutGroup = new PayoutGroup(sellerId, raceId, raceNameOf(raceId),
					sellerAccountPort.hasPayoutAccount(sellerId), group.getValue());
				payoutGroupCreator.create(payoutGroup, targetDate, now);
				created++;
			} catch (RuntimeException e) {
				failed++;
				log.error("정산 내역을 만들지 못했습니다. sellerId={}, raceId={}, 참가권 {}장, targetDate={}",
					sellerId, raceId, group.getValue().size(), targetDate, e);
			}
		}

		log.info("정산 대상일 배치 완료. targetDate={}, 생성 {}, 이미 있음 {}, 실패 {}", targetDate, created, skipped, failed);
	}

	// 이름은 보여 주기용이라, 상품 서비스에 대회가 없어도 정산(돈)은 막지 않는다. 대신 경고를 남겨 데이터를 맞추게 한다
	private String raceNameOf(int raceId) {
		return racePort.findRaceName(raceId).orElseGet(() -> {
			log.warn("상품 서비스에서 대회를 찾지 못해 임시 이름으로 정산 내역을 만듭니다. raceId={}", raceId);
			return UNKNOWN_RACE_NAME.formatted(raceId);
		});
	}
}
