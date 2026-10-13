package com.bukang.payout.app;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.payout.domain.BusinessDayCalendar;
import com.bukang.payout.domain.Payout;
import com.bukang.payout.domain.PayoutHold;
import com.bukang.payout.domain.PayoutHoldReason;
import com.bukang.payout.out.PayoutHoldRepository;
import com.bukang.payout.out.PayoutRepository;
import com.bukang.payout.out.SoldEntry;

import lombok.RequiredArgsConstructor;

/**
 * 정산 대상일 배치의 한 묶음(판매자 × 대회)을 트랜잭션 하나로 저장한다 (PRO-60)
 * 정산 내역 · 정산 항목 · 계좌 미등록 보류는 함께 저장되거나 함께 롤백된다. 보류만 빠지면 계좌 없는 판매자에게 지급될 수 있다
 * PayoutCreateUseCase 안의 메서드로 두면 같은 객체 안의 호출이라 트랜잭션 프록시를 거치지 않으므로 별도 빈으로 둔다
 */
@Service
@RequiredArgsConstructor
public class PayoutGroupCreator {
	// 플랫폼 수수료율 5.5% (부가세 포함). 정산 항목마다 기록 시점 값으로 남긴다
	private static final BigDecimal FEE_RATE = new BigDecimal("0.0550");
	// 자동 보류의 사유 상세. 관리자가 건 보류와 구분되도록 고정 문구를 남긴다
	private static final String AUTO_HOLD_REASON = "정산 계좌 미등록으로 자동 보류";

	private final PayoutRepository payoutRepository;
	private final PayoutHoldRepository payoutHoldRepository;
	private final BusinessDayCalendar businessDayCalendar;

	/**
	 * 묶음 하나의 정산 내역을 만든다. 다른 서비스에 묻는 값(대회 이름, 계좌 등록 여부)은 부르는 쪽이 미리 구해 넘긴다
	 * (다른 서비스 응답을 기다리는 동안 DB 트랜잭션을 열어 두지 않기 위해서다)
	 */
	@Transactional
	public void create(PayoutGroup group, LocalDate targetDate, LocalDateTime now) {
		Payout payout = new Payout(group.sellerId(), group.raceId(), group.raceName(), YearMonth.from(targetDate), now,
			businessDayCalendar);
		group.entries().forEach(entry -> payout.addItem(entry.orderItemId(), entry.price(), FEE_RATE, targetDate));

		// 정산 ID의 끝 4자리는 PK에서 만들므로 저장해서 ID를 받은 뒤에 붙인다
		payoutRepository.save(payout);
		payout.assignPayoutCode();

		// 계좌가 없으면 지급 배치가 고르지 않도록 HELD로 둔다. 판매자가 계좌를 등록하면 관리자가 보류를 푼다
		// 보류는 정산 내역의 PK를 참조하므로 정산 내역을 저장한 뒤에 건다
		if (!group.hasPayoutAccount()) {
			PayoutHold hold = payout.hold(
				PayoutHoldReason.ACCOUNT_NOT_REGISTERED, AUTO_HOLD_REASON, PayoutHold.SYSTEM_MEMBER_ID, now);
			payoutHoldRepository.save(hold);
			hold.assignHoldCode();
		}
	}

	/**
	 * 정산 내역 하나로 묶이는 판매자 × 대회의 참가권과, 저장에 필요한 다른 서비스의 값
	 */
	public record PayoutGroup(int sellerId, int raceId, String raceName, boolean hasPayoutAccount,
		List<SoldEntry> entries) {
	}
}
