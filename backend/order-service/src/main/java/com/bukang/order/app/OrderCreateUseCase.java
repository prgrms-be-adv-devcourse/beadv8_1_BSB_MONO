package com.bukang.order.app;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.bukang.order.domain.Order;
import com.bukang.order.domain.Participation;
import com.bukang.order.domain.exception.ApplicationPeriodException;
import com.bukang.order.domain.exception.OrderConflictException;
import com.bukang.order.domain.exception.ParticipationAccessDeniedException;
import com.bukang.order.domain.exception.ParticipationNotFoundException;
import com.bukang.order.domain.exception.RaceCourseNotFoundException;
import com.bukang.order.out.OrderRepository;
import com.bukang.order.out.ParticipationRepository;
import com.bukang.order.out.race.RaceCourseInfo;
import com.bukang.order.out.race.RaceCoursePort;
import com.bukang.order.out.seat.SeatHoldPort;
import com.bukang.order.out.seat.SeatHoldResult;

import lombok.RequiredArgsConstructor;

/**
 * 바로 결제 예매 ([결제하기], 정책 3장·4장). 상품 서비스(B)에 자리 1석을 확보받고 결제 기한 10분짜리 주문을 만든다
 * 확보하면 참가 신청 PENDING → APPLIED, 주문 CREATED. 매진이면 참가 신청 FAILED, 주문 없음 (ERD 추천안 v4 8·9쪽)
 */
@Service
@RequiredArgsConstructor
public class OrderCreateUseCase {
	// 결제 기한: [결제하기]로 자리를 잡은 때부터 10분 (정책 4장 2-4). 상품 서비스에 이 시각까지 자리를 잡아 달라고 보낸다
	static final long PAYMENT_MINUTES = 10;

	// Order·OrderItem의 @UniqueConstraint 이름과 같아야 한다
	private static final List<String> ORDER_CONSTRAINTS = List.of("uk_order_idempotency",
		"uk_order_item_participation");

	private final ParticipationRepository participationRepository;
	private final OrderRepository orderRepository;
	private final RaceCoursePort raceCoursePort;
	private final SeatHoldPort seatHoldPort;

	public OrderCreateResult create(int memberId, int participationId, String idempotencyKey, LocalDateTime now) {
		// 같은 신청서로 들어온 요청을 줄 세운다. 먼저 끝난 요청이 만든 주문을 뒤 요청이 아래에서 찾는다
		Participation participation = participationRepository.findByIdForUpdate(participationId)
			.orElseThrow(() -> new ParticipationNotFoundException("존재하지 않는 참가 신청입니다."));
		if (!participation.isOwnedBy(memberId)) {
			throw new ParticipationAccessDeniedException("본인의 참가 신청만 결제할 수 있습니다.");
		}
		Optional<Order> existing = orderRepository.findByBuyerIdAndIdempotencyKey(memberId, idempotencyKey);
		if (existing.isPresent()) {
			// 같은 키를 다른 신청서에 다시 쓰면 처음 주문을 돌려주지 않는다 (엉뚱한 주문을 성공으로 알리지 않게)
			if (!existing.get().isFor(participationId)) {
				throw new OrderConflictException("이미 다른 주문에 쓴 Idempotency-Key입니다. 새 키로 다시 요청해주세요.");
			}
			return OrderCreateResult.existing(existing.get().toDto());
		}

		// 매진으로 이미 끝난 신청서로 다시 요청하면(같은 키 재시도 포함) 처음과 같은 매진 결과를 돌려준다
		if (participation.isHoldFailed()) {
			return OrderCreateResult.soldOut();
		}
		participation.checkPayable();
		RaceCourseInfo course = raceCoursePort.findCourse(participation.getRaceCourseId())
			.orElseThrow(() -> new RaceCourseNotFoundException("존재하지 않는 종목입니다."));
		ParticipationApplyUseCase.checkNotCanceled(course);
		if (!course.isAccepting(now)) {
			throw new ApplicationPeriodException("접수 기간이 아닙니다.");
		}

		SeatHoldResult hold = seatHoldPort.hold(course.raceCourseId(), participation.getId(),
			now.plusMinutes(PAYMENT_MINUTES));
		if (!hold.held()) {
			participation.markHoldFailed();
			return OrderCreateResult.soldOut();
		}
		participation.markApplied(hold.expiresAt());
		Order order = Order.forParticipation(participation, course, idempotencyKey, hold.expiresAt());
		try {
			orderRepository.saveAndFlush(order);
		} catch (DataIntegrityViolationException exception) {
			// 줄 세우기를 거쳐도 남는 경합(다른 신청서에 같은 키 등)은 유니크 제약이 막는다
			if (isOrderConflict(exception)) {
				throw new OrderConflictException("같은 주문 요청을 처리하고 있습니다. 잠시 뒤 주문 내역을 확인해주세요.");
			}
			throw exception;
		}
		order.assignOrderNo();
		return OrderCreateResult.created(order.toDto());
	}

	private boolean isOrderConflict(DataIntegrityViolationException exception) {
		String message = exception.getMostSpecificCause().getMessage();
		if (message == null) {
			return false;
		}
		String lowerCaseMessage = message.toLowerCase();
		return ORDER_CONSTRAINTS.stream().anyMatch(lowerCaseMessage::contains);
	}
}
