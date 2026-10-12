package com.bukang.order.out.seat;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 임시 어댑터: 상품 서비스(B)가 아직 없어, 종목마다 정원 100석을 메모리에 두고 지금부터 10분짜리 자리를 내준다
 * 확보한 자리의 기한을 기억해 2분 연장(한 번)과 기한 안 판매까지 흉내 낸다. 자리를 되돌려 놓지는 않는다
 * 앱을 다시 켜면 처음으로 돌아간다. 상품 서비스와 연결할 때(HTTP 호출, PRO-99) 이 클래스를 진짜 어댑터로 바꾼다
 */
@Slf4j
@Component
public class FakeSeatHoldAdapter implements SeatHoldPort {
	private static final int SEATS_PER_COURSE = 100;
	private static final long PAYMENT_MINUTES = 10;
	private static final long EXTEND_MINUTES = 2;

	private final Map<Integer, Integer> remainingSeats = new ConcurrentHashMap<>();
	private final Map<Integer, Hold> holds = new ConcurrentHashMap<>();
	private final Set<Integer> sold = ConcurrentHashMap.newKeySet();

	// 호출마다 경고를 남기면 로그가 묻히므로, 앱을 켤 때 한 번만 알린다
	public FakeSeatHoldAdapter() {
		log.warn("상품 서비스 연결 전이라 자리 확보는 가짜 응답(FakeSeatHoldAdapter)을 씁니다.");
	}

	@Override
	public SeatHoldResult hold(int raceCourseId, int participationId, int memberId) {
		// "남은 자리가 1 이상일 때만 줄인다"를 종목별로 원자적으로 처리한다 (B의 조건부 UPDATE 흉내)
		boolean[] held = {false};
		remainingSeats.compute(raceCourseId, (id, remaining) -> {
			int seats = remaining == null ? SEATS_PER_COURSE : remaining;
			if (seats <= 0) {
				return seats;
			}
			held[0] = true;
			return seats - 1;
		});
		log.debug("가짜 자리 확보 raceCourseId={}, participationId={}, held={}", raceCourseId, participationId, held[0]);
		if (!held[0]) {
			return SeatHoldResult.soldOut();
		}
		LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(PAYMENT_MINUTES);
		holds.put(participationId, new Hold(expiresAt, false));
		return SeatHoldResult.held(expiresAt);
	}

	@Override
	public SeatExtendResult extend(int participationId) {
		SeatExtendResult[] result = {SeatExtendResult.expired()};
		holds.computeIfPresent(participationId, (id, hold) -> {
			if (hold.extended()) {
				result[0] = SeatExtendResult.alreadyExtended();
				return hold;
			}
			if (!LocalDateTime.now().isBefore(hold.expiresAt())) {
				return hold;
			}
			Hold extended = new Hold(hold.expiresAt().plusMinutes(EXTEND_MINUTES), true);
			result[0] = SeatExtendResult.extended(extended.expiresAt());
			return extended;
		});
		return result[0];
	}

	@Override
	public SeatSellResult sell(int participationId) {
		// 이미 판매한 자리는 기한과 상관없이 SOLD (같은 주문 재결제, SeatHoldPort.sell)
		if (sold.contains(participationId)) {
			return SeatSellResult.SOLD;
		}
		Hold hold = holds.get(participationId);
		if (hold == null || !LocalDateTime.now().isBefore(hold.expiresAt())) {
			return SeatSellResult.EXPIRED;
		}
		holds.remove(participationId);
		sold.add(participationId);
		return SeatSellResult.SOLD;
	}

	private record Hold(LocalDateTime expiresAt, boolean extended) {
	}
}
