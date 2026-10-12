package com.bukang.order.support;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.bukang.order.out.seat.SeatExtendResult;
import com.bukang.order.out.seat.SeatHoldPort;
import com.bukang.order.out.seat.SeatHoldResult;

/**
 * 테스트용 자리 확보. 종목별 남은 자리를 정할 수 있고(정하지 않으면 무제한), 결제 기한은 지금부터 10분이다
 * 연장은 자리마다 한 번, 2분이다. 확보하지 않은 자리를 늘리려 하면 기한이 지난 것으로 본다
 */
public class StubSeatHoldPort implements SeatHoldPort {
	public static final long PAYMENT_MINUTES = 10;
	public static final long EXTEND_MINUTES = 2;

	private final Map<Integer, Integer> remainingSeats = new ConcurrentHashMap<>();
	private final AtomicInteger holdCalls = new AtomicInteger();
	private final AtomicInteger extendCalls = new AtomicInteger();
	private final Map<Integer, LocalDateTime> expiresAt = new ConcurrentHashMap<>();
	private final Map<Integer, Boolean> extended = new ConcurrentHashMap<>();

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
		if (!held[0]) {
			return SeatHoldResult.soldOut();
		}
		LocalDateTime due = LocalDateTime.now().plusMinutes(PAYMENT_MINUTES);
		expiresAt.put(participationId, due);
		return SeatHoldResult.held(due);
	}

	@Override
	public SeatExtendResult extend(int participationId) {
		extendCalls.incrementAndGet();
		LocalDateTime due = expiresAt.get(participationId);
		if (due == null) {
			return SeatExtendResult.expired();
		}
		if (extended.putIfAbsent(participationId, true) != null) {
			return SeatExtendResult.alreadyExtended();
		}
		LocalDateTime newDue = due.plusMinutes(EXTEND_MINUTES);
		expiresAt.put(participationId, newDue);
		return SeatExtendResult.extended(newDue);
	}

	public int extendCalls() {
		return extendCalls.get();
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
		extendCalls.set(0);
		expiresAt.clear();
		extended.clear();
	}
}
