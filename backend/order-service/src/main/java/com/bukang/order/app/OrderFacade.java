package com.bukang.order.app;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.order.dto.OrderCreateRequestDto;
import com.bukang.order.dto.OrderDto;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderFacade {
	private final OrderCreateUseCase orderCreateUseCase;
	private final OrderDeadlineExtendUseCase orderDeadlineExtendUseCase;

	@Transactional
	public OrderCreateResult create(OrderCreateRequestDto request, String idempotencyKey, int memberId) {
		return orderCreateUseCase.create(memberId, request.getParticipationId(), idempotencyKey,
			LocalDateTime.now());
	}

	@Transactional
	public OrderDto extendDeadline(int orderId, int memberId) {
		return orderDeadlineExtendUseCase.extend(memberId, orderId, LocalDateTime.now());
	}
}
