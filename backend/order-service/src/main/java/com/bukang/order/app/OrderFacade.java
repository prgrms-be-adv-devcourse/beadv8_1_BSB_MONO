package com.bukang.order.app;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.order.dto.OrderCreateRequestDto;
import com.bukang.order.dto.OrderDto;
import com.bukang.order.dto.OrderPageDto;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderFacade {
	private final OrderCreateUseCase orderCreateUseCase;
	private final OrderDeadlineExtendUseCase orderDeadlineExtendUseCase;
	private final OrderQueryUseCase orderQueryUseCase;

	@Transactional
	public OrderCreateResult create(OrderCreateRequestDto request, String idempotencyKey, int memberId) {
		return orderCreateUseCase.create(memberId, request.getParticipationId(), idempotencyKey,
			LocalDateTime.now());
	}

	@Transactional
	public OrderDto extendDeadline(int orderId, int memberId) {
		return orderDeadlineExtendUseCase.extend(memberId, orderId, LocalDateTime.now());
	}

	public OrderPageDto findMine(int memberId, LocalDate from, LocalDate to, int page, int size) {
		return orderQueryUseCase.findMine(memberId, from, to, page, size);
	}

	public OrderDto findById(int orderId, int memberId) {
		return orderQueryUseCase.findOwned(memberId, orderId);
	}
}
