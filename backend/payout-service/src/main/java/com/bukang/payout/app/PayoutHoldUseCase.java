package com.bukang.payout.app;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.payout.domain.BusinessDayCalendar;
import com.bukang.payout.domain.Payout;
import com.bukang.payout.domain.PayoutHold;
import com.bukang.payout.domain.PayoutHoldReason;
import com.bukang.payout.domain.exception.PayoutHoldNotFoundException;
import com.bukang.payout.domain.exception.PayoutNotFoundException;
import com.bukang.payout.dto.PayoutHoldDto;
import com.bukang.payout.out.PayoutHoldRepository;
import com.bukang.payout.out.PayoutRepository;

import lombok.RequiredArgsConstructor;

/**
 * 관리자의 지급보류 걸기 · 풀기 (payout.md 1장 지급보류, 2-5)
 * 상태 규칙은 Payout이 지키고, 여기서는 찾아서 넘기고 저장만 한다
 * 지급 배치와 동시에 같은 정산 내역을 바꾸면 Payout의 @Version으로 늦게 커밋한 쪽이 실패한다
 */
@Service
@RequiredArgsConstructor
public class PayoutHoldUseCase {
	private final PayoutRepository payoutRepository;
	private final PayoutHoldRepository payoutHoldRepository;
	private final BusinessDayCalendar businessDayCalendar;

	// open-in-view가 꺼져 있어 트랜잭션 밖에서 hold.getPayout()을 읽지 않도록, 안에서 DTO로 바꿔 돌려준다
	@Transactional
	public PayoutHoldDto hold(int payoutId, PayoutHoldReason reasonType, String reason, int heldBy,
		LocalDateTime now) {
		Payout payout = payoutRepository.findById(payoutId).orElseThrow(PayoutNotFoundException::new);
		PayoutHold hold = payout.hold(reasonType, reason, heldBy, now);

		// 지급보류 ID의 끝 4자리는 PK에서 만들므로 저장해서 ID를 받은 뒤에 붙인다
		payoutHoldRepository.save(hold);
		hold.assignHoldCode();
		return PayoutHoldDto.from(hold);
	}

	@Transactional
	public PayoutHoldDto release(int holdId, int releasedBy, LocalDateTime now) {
		PayoutHold hold = payoutHoldRepository.findById(holdId).orElseThrow(PayoutHoldNotFoundException::new);
		hold.getPayout().release(hold, releasedBy, now, businessDayCalendar);
		return PayoutHoldDto.from(hold);
	}
}
