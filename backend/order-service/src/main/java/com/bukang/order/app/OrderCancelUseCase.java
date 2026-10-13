package com.bukang.order.app;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.bukang.order.domain.Order;
import com.bukang.order.domain.OrderItem;
import com.bukang.order.domain.Participation;
import com.bukang.order.domain.exception.OrderAccessDeniedException;
import com.bukang.order.domain.exception.OrderNotFoundException;
import com.bukang.order.domain.exception.ParticipationNotFoundException;
import com.bukang.order.domain.exception.RaceCourseNotFoundException;
import com.bukang.order.domain.exception.RefundNotAllowedException;
import com.bukang.order.dto.OrderDto;
import com.bukang.order.out.OrderRepository;
import com.bukang.order.out.ParticipationRepository;
import com.bukang.order.out.payout.RefundEligibilityPort;
import com.bukang.order.out.race.RaceCourseInfo;
import com.bukang.order.out.race.RaceCoursePort;
import com.bukang.order.out.seat.SeatHoldPort;
import com.bukang.order.out.wallet.WalletPort;

import lombok.RequiredArgsConstructor;

/**
 * 결제한 참가권을 주문 상품 단위로 취소·환불한다 (정책 5장). 키트는 같은 주문 상품이라 함께 취소된다
 * 환불 기한 안이거나, 대회 일정이 바뀐 뒤 7일 안이면 자동 승인 (ERD v4 환불 판정):
 * 주문 상품 CANCELED, 주문 PARTIAL_CANCELED/CANCELED, 참가 신청 CANCELED
 * → 자리 되돌리기(B) → 예치금 환불(D). 정산 대상일이 지난 참가권은 정산 서비스(E)가 거절한다
 * 외부 호출은 모두 주문 상품·참가 신청 단위로 멱등이라, 중간에 실패해 롤백되면 같은 취소를 다시 요청하면 된다
 */
@Service
@RequiredArgsConstructor
public class OrderCancelUseCase {
	private final OrderRepository orderRepository;
	private final ParticipationRepository participationRepository;
	private final RaceCoursePort raceCoursePort;
	private final RefundEligibilityPort refundEligibilityPort;
	private final SeatHoldPort seatHoldPort;
	private final WalletPort walletPort;

	public record CancelResult(OrderDto order, int refundedAmount) {
	}

	public CancelResult cancel(int memberId, int orderId, int orderItemId, LocalDateTime now) {
		// 주문을 먼저 잠근다. 같은 상품 취소가 동시에 와도 한 요청씩 처리되어 환불이 한 번만 일어난다
		Order order = orderRepository.findByIdForUpdate(orderId)
			.orElseThrow(() -> new OrderNotFoundException("존재하지 않는 주문입니다."));
		if (!order.isOwnedBy(memberId)) {
			throw new OrderAccessDeniedException("본인의 주문만 취소할 수 있습니다.");
		}
		OrderItem item = order.findItem(orderItemId);
		item.checkCancelable();

		RaceCourseInfo course = raceCoursePort.findCourse(item.getRaceCourseId())
			.orElseThrow(() -> new RaceCourseNotFoundException("존재하지 않는 종목입니다."));
		if (!course.isRefundableOn(now.toLocalDate())) {
			throw new RefundNotAllowedException(refundPeriodPassedMessage(course));
		}
		// 정산 대상일은 상품 서비스의 지금 대회 일시로 따진다. 주문 상품의 대회 일시는 주문 때 복사한 값이라
		// 일정이 미뤄졌으면 옛 날짜로 남아 있어, 일정 변경 뒤 7일 환불을 정산 시작으로 잘못 거절한다
		if (!refundEligibilityPort.isRefundable(course.raceDate().toLocalDate())) {
			throw new RefundNotAllowedException("정산이 시작된 참가권이라 취소할 수 없습니다.");
		}

		// 참가 신청도 잠근다. 참가자 정보 수정과 겹쳐도 CANCELED가 옛 상태로 덮이지 않는다 (잠금 순서: 주문 → 참가 신청)
		Participation participation = participationRepository.findByIdForUpdate(item.getParticipationId())
			.orElseThrow(() -> new ParticipationNotFoundException("존재하지 않는 참가 신청입니다."));
		order.cancelItem(item, now);
		participation.markCanceled(now);

		seatHoldPort.restore(item.getParticipationId());
		walletPort.refund(order.getId(), item.getId(), item.getAmount());
		return new CancelResult(order.toDto(), item.getAmount());
	}

	private String refundPeriodPassedMessage(RaceCourseInfo course) {
		LocalDate scheduleChangeLastDay = course.scheduleChangeRefundLastDay();
		if (scheduleChangeLastDay == null) {
			return "환불 기한(%s)이 지나 취소할 수 없습니다.".formatted(course.refundDeadline());
		}
		return "환불 기한(%s)과 일정 변경 뒤 전액 환불 기간(%s까지)이 지나 취소할 수 없습니다."
			.formatted(course.refundDeadline(), scheduleChangeLastDay);
	}
}
