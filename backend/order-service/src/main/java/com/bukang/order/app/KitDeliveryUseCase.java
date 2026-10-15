package com.bukang.order.app;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.bukang.order.domain.Order;
import com.bukang.order.domain.OrderItem;
import com.bukang.order.domain.exception.KitNotShippableException;
import com.bukang.order.domain.exception.OrderAccessDeniedException;
import com.bukang.order.domain.exception.OrderItemNotFoundException;
import com.bukang.order.domain.exception.RaceCourseNotFoundException;
import com.bukang.order.dto.OrderItemDto;
import com.bukang.order.out.OrderItemRepository;
import com.bukang.order.out.OrderRepository;
import com.bukang.order.out.race.RaceCourseInfo;
import com.bukang.order.out.race.RaceCoursePort;

import lombok.RequiredArgsConstructor;

/**
 * 판매자가 키트를 발송 처리한다 (PRO-136, 정책 5장 3 키트: 키트는 환불 기한이 지난 뒤 배송하고 배송 상태는 판매자가 바꾼다)
 * 환불 기한 안에는 참가권을 취소할 수 있어 아직 보내지 않는다. 기한이 지난 뒤 보내야 보낸 키트를 돌려받을 일이 없다
 * (대회 취소·일정 변경으로 기한 뒤에 환불돼도 이미 받은 키트는 돌려받지 않는다, 정책 5장 4-3)
 * 트랜잭션은 이 클래스가 연다. 종목 정보(상품 HTTP)는 주문을 잠그기 전에 묻는다 (OrderCancelUseCase와 같은 방식)
 */
@Service
@RequiredArgsConstructor
public class KitDeliveryUseCase {
	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final RaceCoursePort raceCoursePort;
	private final TransactionTemplate transactionTemplate;

	/**
	 * @param sellerId 판매자 ID (판매자 테이블 ID, 주문 상품의 seller_id와 같은 값)
	 */
	public OrderItemDto ship(int sellerId, int orderItemId, LocalDateTime now) {
		OrderItem target = transactionTemplate.execute(status -> findSellersItem(sellerId, orderItemId));
		RaceCourseInfo course = raceCoursePort.findCourse(target.getRaceCourseId())
			.orElseThrow(() -> new RaceCourseNotFoundException("존재하지 않는 종목입니다."));
		if (!now.toLocalDate().isAfter(course.refundDeadline())) {
			throw new KitNotShippableException(
				"환불 기한(%s)이 지난 뒤 발송할 수 있습니다.".formatted(course.refundDeadline()));
		}
		return transactionTemplate.execute(status -> shipLocked(sellerId, orderItemId, now));
	}

	private OrderItem findSellersItem(int sellerId, int orderItemId) {
		int orderId = orderItemRepository.findOrderIdByItemId(orderItemId)
			.orElseThrow(() -> new OrderItemNotFoundException("주문에 없는 상품입니다."));
		OrderItem item = orderRepository.findById(orderId).orElseThrow().findItem(orderItemId);
		if (!item.isSoldBy(sellerId)) {
			throw new OrderAccessDeniedException("본인 대회의 참가권만 발송 처리할 수 있습니다.");
		}
		// 주문 상품만 보고 알 수 있는 이유(키트 없음, 이미 보냄, 결제 전)는 상품 서비스에 묻기 전에 알린다
		item.checkKitShippable();
		return item;
	}

	// 주문을 잠그고 바꾼다. 같은 참가권의 취소와 겹쳐도 한 요청씩 처리되어, 취소된 참가권은 발송하지 않는다
	private OrderItemDto shipLocked(int sellerId, int orderItemId, LocalDateTime now) {
		Order order = orderRepository.findByIdForUpdate(orderItemRepository.findOrderIdByItemId(orderItemId)
			.orElseThrow(() -> new OrderItemNotFoundException("주문에 없는 상품입니다."))).orElseThrow();
		OrderItem item = order.findItem(orderItemId);
		if (!item.isSoldBy(sellerId)) {
			throw new OrderAccessDeniedException("본인 대회의 참가권만 발송 처리할 수 있습니다.");
		}
		item.shipKit(now);
		return item.toDto();
	}
}
