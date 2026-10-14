package com.bukang.order.app;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.bukang.order.domain.Order;
import com.bukang.order.domain.OrderItem;
import com.bukang.order.domain.Participation;
import com.bukang.order.domain.exception.ParticipationNotFoundException;
import com.bukang.order.out.OrderRepository;
import com.bukang.order.out.ParticipationRepository;
import com.bukang.order.out.wallet.WalletPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 판매자가 대회를 취소하면 그 대회의 결제한 참가권을 모두 취소하고 전액 환불한다 (PRO-135, 정책 1차 5장 4번)
 * 주문마다: 주문 잠금 → 결제한 주문 상품 CANCELED, 주문 CANCELED → 참가 신청 CANCELED → 예치금 환불(D, 주문 ID 멱등)
 * 환불 기한·정산 대상일은 보지 않는다. 대회 취소는 기한과 상관없이 전액 환불이고, 정산은 대회 취소로 보류된다 (PRO-34)
 * 자리는 되돌리지 않는다. 취소된 대회는 다시 팔지 않고, 결제 대기 자리는 상품 서비스가 취소할 때 함께 푼다
 * 결제 대기 주문은 그대로 둔다. 상품이 자리를 풀었으므로 결제하면 판매가 거절되고, 기한이 지나면 만료 정리가 끝낸다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RaceCancelRefundUseCase {
	// 한 번에 읽을 주문 ID 수. 대회 하나의 주문이 많아도 ID 목록을 한꺼번에 메모리에 올리지 않는다
	static final int BATCH_SIZE = 100;

	private final OrderRepository orderRepository;
	private final ParticipationRepository participationRepository;
	private final WalletPort walletPort;
	private final TransactionTemplate transactionTemplate;

	/**
	 * @param refundedOrders 이번에 취소·환불한 주문 수
	 * @param failedOrders   환불(D 장애 등)에 실패해 결제 완료로 남긴 주문 수. 같은 대회 취소를 다시 처리하면 이어서 환불한다
	 */
	public record RefundResult(int refundedOrders, int failedOrders) {
	}

	/**
	 * 주문마다 트랜잭션을 따로 연다. 한 주문의 환불이 실패해도 그 주문만 롤백되고 나머지는 계속 환불한다
	 * 이미 취소한 주문은 건너뛰므로, 같은 대회로 여러 번 불러도 주문마다 한 번만 환불한다
	 * 바깥 트랜잭션 없이 부른다 (OrderKafkaConsumer)
	 */
	public RefundResult refundAll(int raceId, LocalDateTime now) {
		int refunded = 0;
		int failed = 0;
		int afterId = 0;
		List<Integer> orderIds;
		while (!(orderIds = orderRepository.findRefundCandidateIds(raceId, afterId,
			PageRequest.of(0, BATCH_SIZE))).isEmpty()) {
			for (int orderId : orderIds) {
				afterId = orderId;
				try {
					if (Boolean.TRUE.equals(transactionTemplate.execute(status -> refundLocked(orderId, now)))) {
						refunded++;
					}
				} catch (RuntimeException exception) {
					failed++;
					log.warn("취소된 대회의 주문을 환불하지 못했습니다. raceId={}, orderId={}, cause={}",
						raceId, orderId, exception.toString());
				}
			}
		}
		log.info("취소된 대회의 참가권을 환불했습니다. raceId={}, 환불 {}건, 실패 {}건", raceId, refunded, failed);
		return new RefundResult(refunded, failed);
	}

	// 주문 → 참가 신청 순서로 잠근다 (결제·취소·만료 정리와 같은 순서). 결제 중인 주문이면 결제가 끝날 때까지 기다린다
	private boolean refundLocked(int orderId, LocalDateTime now) {
		Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
		if (order == null) {
			return false;
		}
		List<OrderItem> canceledItems = order.cancelPaidItems(now);
		if (canceledItems.isEmpty()) {
			return false;
		}
		for (OrderItem item : canceledItems) {
			Participation participation = participationRepository.findByIdForUpdate(item.getParticipationId())
				.orElseThrow(() -> new ParticipationNotFoundException("존재하지 않는 참가 신청입니다."));
			participation.markCanceled(now);
		}
		// 주문 하나 = 참가권 하나라 주문 단위 전액 환불이 그 참가권의 환불이다. 실패하면 예외로 이 주문만 롤백된다
		walletPort.refund(order.getId(), order.getBuyerId());
		return true;
	}
}
