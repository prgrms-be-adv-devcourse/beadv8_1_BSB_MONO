package com.bukang.order.support;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.bukang.order.domain.exception.ExternalServiceUnavailableException;
import com.bukang.order.out.seat.SeatHoldPort;
import com.bukang.order.out.seat.SeatHoldResult;
import com.bukang.order.out.seat.SeatSellResult;

/**
 * 테스트용 자리 확보. 종목별 남은 자리를 정할 수 있고(정하지 않으면 무제한), 결제 기한은 주문이 보낸 값(대기열 입장 끝 시각)이다
 */
public class StubSeatHoldPort implements SeatHoldPort {
	private final Map<Integer, Integer> remainingSeats = new ConcurrentHashMap<>();
	private final AtomicInteger holdCalls = new AtomicInteger();
	private final List<Integer> restoredParticipations = new CopyOnWriteArrayList<>();
	private final List<Integer> releasedParticipations = new CopyOnWriteArrayList<>();
	private volatile boolean restoreFails;
	private volatile boolean releaseFails;
	private final Map<Integer, LocalDateTime> expiresAt = new ConcurrentHashMap<>();
	private final Set<Integer> sold = ConcurrentHashMap.newKeySet();
	// 자리 확보 때 예상하지 못한 오류(타임아웃 등)를 낼 종목
	private final Set<Integer> failingCourses = ConcurrentHashMap.newKeySet();

	@Override
	public SeatHoldResult hold(int raceCourseId, int participationId, LocalDateTime requestedExpiresAt) {
		holdCalls.incrementAndGet();
		if (failingCourses.contains(raceCourseId)) {
			throw new IllegalStateException("상품 서비스 응답 없음 (테스트)");
		}
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
		expiresAt.put(participationId, requestedExpiresAt);
		return SeatHoldResult.held(requestedExpiresAt);
	}

	@Override
	public SeatSellResult sell(int participationId) {
		// 이미 판매한 자리는 기한과 상관없이 SOLD (SeatHoldPort.sell의 멱등 계약)
		if (sold.contains(participationId)) {
			return SeatSellResult.SOLD;
		}
		LocalDateTime due = expiresAt.get(participationId);
		if (due == null || !LocalDateTime.now().isBefore(due)) {
			return SeatSellResult.EXPIRED;
		}
		sold.add(participationId);
		return SeatSellResult.SOLD;
	}

	// 상품 서비스 쪽 기한만 지나게 만든다 (주문 쪽 사본은 그대로). 결제 직전 만료 상황을 재현할 때 쓴다
	public void expire(int participationId) {
		expiresAt.put(participationId, LocalDateTime.now().minusSeconds(1));
	}

	@Override
	public void restore(int participationId) {
		if (restoreFails) {
			throw new IllegalStateException("상품 서비스 응답 없음 (테스트)");
		}
		restoredParticipations.add(participationId);
	}

	// 자리 해제 메시지를 보내지 못하게 한다 (Kafka 장애)
	public void failRelease(boolean fails) {
		this.releaseFails = fails;
	}

	// 자리 복구가 실패하게 한다 (상품 서비스 장애)
	public void failRestore(boolean fails) {
		this.restoreFails = fails;
	}

	public List<Integer> restoredParticipations() {
		return restoredParticipations;
	}

	// 결제하지 않은 자리를 풀어 달라는 알림 (B의 해제처럼 판매한 자리는 그대로 둔다)
	@Override
	public void release(int participationId) {
		if (releaseFails) {
			throw new ExternalServiceUnavailableException("상품 서비스에 연결하지 못했습니다. (테스트)");
		}
		releasedParticipations.add(participationId);
		if (!sold.contains(participationId)) {
			expiresAt.remove(participationId);
		}
	}

	public List<Integer> releasedParticipations() {
		return releasedParticipations;
	}

	// 판매(HELD → SOLD)까지 끝난 자리인지
	public boolean isSold(int participationId) {
		return sold.contains(participationId);
	}

	public void failHold(int raceCourseId) {
		failingCourses.add(raceCourseId);
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
		restoredParticipations.clear();
		releasedParticipations.clear();
		restoreFails = false;
		releaseFails = false;
		expiresAt.clear();
		sold.clear();
		failingCourses.clear();
	}
}
