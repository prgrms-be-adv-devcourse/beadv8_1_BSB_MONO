package com.bukang.order.app;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.order.out.OrderRepository;
import com.bukang.order.out.ParticipationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 결제 기한(10분, 연장 시 +2분)이 지난 결제 대기 건을 정리한다 (정책 4장 결제, ERD 추천안 v4 9쪽 6번)
 * 참가 신청 APPLIED → RELEASED, 주문 CREATED → EXPIRED. 자리 해제·정원 복구는 상품 서비스(B)가 자기 expires_at으로 한다
 * 여기의 payment_due_at은 화면·정리용 사본이라, 정리가 몇 분 늦어도 결제 판정에는 영향이 없다 (판정 원본은 B)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderExpiryUseCase {
	private final ParticipationRepository participationRepository;
	private final OrderRepository orderRepository;

	public record ExpiryResult(int releasedParticipations, int expiredOrders) {
	}

	@Transactional
	public ExpiryResult expireOverdue(LocalDateTime now) {
		// 주문 → 참가 신청 순서로 바꾼다. 결제 기한 늘리기(OrderDeadlineExtendUseCase)와 잠그는 순서를 같게 해
		// 기한 직전에 둘이 겹쳐도 서로 기다리다 멈추지 않는다. 늘리기가 먼저 끝나면 늘어난 기한이라 이 조건에 걸리지 않는다
		int expired = orderRepository.expireOverdue(now);
		int released = participationRepository.releaseOverdue(now);
		if (released > 0 || expired > 0) {
			log.info("결제 기한이 지난 건을 정리했습니다. 참가 신청 {}건, 주문 {}건", released, expired);
		}
		return new ExpiryResult(released, expired);
	}
}
