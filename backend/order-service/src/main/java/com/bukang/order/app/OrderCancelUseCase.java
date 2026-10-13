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
		if (!isBeforePayout(item.getRaceDate().toLocalDate(), course.raceDate().toLocalDate())) {
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

	/**
	 * 정산 대상일 전인지 두 날짜로 모두 묻는다. MVP는 회수가 없어, 정산에 들어간 참가권을 환불하면 판매자 지급과
	 * 구매자 환불이 둘 다 나간다 (payout PayoutTargetDate)
	 * - 주문 상품의 대회 일시: 정산이 판매 내역을 고를 때 쓰는 날짜다. 주문 때 복사해 일정이 바뀌어도 그대로라,
	 *   대회가 미뤄졌으면 이 옛 날짜로 이미 정산됐을 수 있다
	 * - 상품 서비스의 지금 대회 일시: 대회가 당겨졌으면 실제 대회가 이미 끝났을 수 있다
	 * 일정이 바뀔 때 주문 상품의 대회 일시도 함께 고치면 한 날짜로 줄일 수 있다 (pending-items 코드 14)
	 */
	private boolean isBeforePayout(LocalDate copiedRaceDate, LocalDate currentRaceDate) {
		if (!refundEligibilityPort.isRefundable(copiedRaceDate)) {
			return false;
		}
		return copiedRaceDate.equals(currentRaceDate) || refundEligibilityPort.isRefundable(currentRaceDate);
	}

	// 일정 변경 뒤 전액 환불 기간이 환불 기한보다 늦게 끝났을 때만 그 기간을 함께 알린다 (먼저 끝났으면 헷갈리기만 한다)
	private String refundPeriodPassedMessage(RaceCourseInfo course) {
		LocalDate scheduleChangeLastDay = course.scheduleChangeRefundLastDay();
		if (scheduleChangeLastDay == null || !scheduleChangeLastDay.isAfter(course.refundDeadline())) {
			return "환불 기한(%s)이 지나 취소할 수 없습니다.".formatted(course.refundDeadline());
		}
		return "환불 기한(%s)과 일정 변경 뒤 전액 환불 기간(%s까지)이 지나 취소할 수 없습니다."
			.formatted(course.refundDeadline(), scheduleChangeLastDay);
	}
}
