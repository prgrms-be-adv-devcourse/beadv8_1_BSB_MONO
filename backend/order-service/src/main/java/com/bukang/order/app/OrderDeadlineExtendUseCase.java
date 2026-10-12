package com.bukang.order.app;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.bukang.order.domain.Order;
import com.bukang.order.domain.Participation;
import com.bukang.order.domain.exception.OrderAccessDeniedException;
import com.bukang.order.domain.exception.OrderNotExtendableException;
import com.bukang.order.domain.exception.OrderNotFoundException;
import com.bukang.order.domain.exception.ParticipationNotFoundException;
import com.bukang.order.dto.OrderDto;
import com.bukang.order.out.OrderRepository;
import com.bukang.order.out.ParticipationRepository;
import com.bukang.order.out.seat.SeatExtendResult;
import com.bukang.order.out.seat.SeatHoldPort;

import lombok.RequiredArgsConstructor;

/**
 * PG 충전창에 들어가면 결제 기한을 2분 늘린다. 주문당 한 번이다 (정책 4장 결제)
 * 상품 서비스(B)의 자리 기한을 먼저 늘리고, 성공하면 주문·참가 신청의 결제 기한 사본을 같은 트랜잭션에서 함께 늦춘다 (09 ERD orders.payment_due_at)
 */
@Service
@RequiredArgsConstructor
public class OrderDeadlineExtendUseCase {
	private final OrderRepository orderRepository;
	private final ParticipationRepository participationRepository;
	private final SeatHoldPort seatHoldPort;

	public OrderDto extend(int memberId, int orderId, LocalDateTime now) {
		// 주문을 먼저 잠근다. 만료 정리 배치도 주문 → 참가 신청 순서로 바꾼다 (OrderExpiryUseCase)
		Order order = orderRepository.findByIdForUpdate(orderId)
			.orElseThrow(() -> new OrderNotFoundException("존재하지 않는 주문입니다."));
		if (!order.isOwnedBy(memberId)) {
			throw new OrderAccessDeniedException("본인의 주문만 결제 기한을 늘릴 수 있습니다.");
		}
		// 상품 서비스를 부르기 전에 이쪽에서 막을 수 있는 경우를 먼저 거른다
		order.checkExtendable(now);

		SeatExtendResult result = seatHoldPort.extend(order.participationId());
		if (result.outcome() == SeatExtendResult.Outcome.ALREADY_EXTENDED) {
			throw new OrderNotExtendableException("결제 기한은 한 번만 늘릴 수 있습니다.");
		}
		if (result.outcome() == SeatExtendResult.Outcome.EXPIRED) {
			throw new OrderNotExtendableException("결제 기한이 지났습니다.");
		}
		Participation participation = participationRepository.findById(order.participationId())
			.orElseThrow(() -> new ParticipationNotFoundException("존재하지 않는 참가 신청입니다."));
		order.extendDeadline(result.expiresAt(), now);
		participation.extendPaymentDue(result.expiresAt());
		return order.toDto();
	}
}
