package com.bukang.order.support;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.bukang.order.out.seat.SeatHoldPort;
import com.bukang.order.out.seat.SeatHoldResult;

/**
 * 테스트용 자리 확보. 종목별 남은 자리를 정할 수 있고(정하지 않으면 무제한), 결제 기한은 지금부터 10분이다
 */
public class StubSeatHoldPort implements SeatHoldPort {
	public static final long PAYMENT_MINUTES = 10;

	private final Map<Integer, Integer> remainingSeats = new ConcurrentHashMap<>();
	private final AtomicInteger holdCalls = new AtomicInteger();

	@Override
	public SeatHoldResult hold(int raceCourseId, int participationId, int memberId) {
		holdCalls.incrementAndGet();
		boolean[] held = {true};
		remainingSeats.computeIfPresent(raceCourseId, (id, remaining) -> {
			if (remaining <= 0) {
				held[0] = false;
				return remaining;
			}
			return remaining - 1;
		});
		return held[0] ? SeatHoldResult.held(LocalDateTime.now().plusMinutes(PAYMENT_MINUTES))
			: SeatHoldResult.soldOut();
	}

	public void setRemainingSeats(int raceCourseId, int seats) {
		remainingSeats.put(raceCourseId, seats);
	}

	public int holdCalls() {
		return holdCalls.get();
	}

	public void clear() {
		remainingSeats.clear();
		holdCalls.set(0);
	}
}
