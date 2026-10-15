package com.bukang.order.app;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.bukang.order.domain.KitDeliveryStatus;
import com.bukang.order.domain.Order;
import com.bukang.order.domain.OrderItem;
import com.bukang.order.domain.exception.KitNotShippableException;
import com.bukang.order.domain.exception.OrderAccessDeniedException;
import com.bukang.order.domain.exception.OrderItemNotFoundException;
import com.bukang.order.domain.exception.RaceCourseNotFoundException;
import com.bukang.order.dto.OrderItemDto;
import com.bukang.order.dto.SellerKitDto;
import com.bukang.order.dto.SellerKitSummaryDto;
import com.bukang.order.out.OrderItemRepository;
import com.bukang.order.out.OrderRepository;
import com.bukang.order.out.race.RaceCourseInfo;
import com.bukang.order.out.race.RaceCoursePort;

import lombok.RequiredArgsConstructor;

/**
 * 판매자의 키트 배송 관리 (PRO-136, 정책 5장 3 키트: 키트는 환불 기한이 지난 뒤 배송하고 배송 상태는 판매자가 바꾼다, 시안 S08)
 * 환불 기한 안에는 참가권을 취소할 수 있어 아직 보내지 않는다. 기한이 지난 뒤 보내야 보낸 키트를 돌려받을 일이 없다
 * (대회 취소·일정 변경으로 기한 뒤에 환불돼도 이미 받은 키트는 돌려받지 않는다, 정책 5장 4-3)
 * 상태를 바꾸는 트랜잭션은 이 클래스가 연다. 종목 정보(상품 HTTP)는 주문을 잠그기 전에 묻는다 (OrderCancelUseCase와 같은 방식)
 * sellerId는 판매자 테이블 ID(주문 상품의 seller_id와 같은 값)다. 회원 ID로 판매자 ID를 찾는 일은 부르는 쪽이 한다
 */
@Service
@RequiredArgsConstructor
public class KitDeliveryUseCase {
	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final RaceCoursePort raceCoursePort;
	private final TransactionTemplate transactionTemplate;

	// 배송 준비 → 배송 중. 택배사·송장 번호를 함께 남긴다
	public OrderItemDto ship(int sellerId, int orderItemId, String carrier, String trackingNumber,
		LocalDateTime now) {
		OrderItem target = transactionTemplate.execute(status -> {
			OrderItem item = findSellersItem(sellerId, orderItemId);
			// 주문 상품만 보고 알 수 있는 이유(키트 없음, 이미 보냄, 결제 전)는 상품 서비스에 묻기 전에 알린다
			item.checkKitShippable();
			return item;
		});
		LocalDate refundDeadline = findCourse(target.getRaceCourseId()).refundDeadline();
		if (!isRefundDeadlinePassed(refundDeadline, now)) {
			throw new KitNotShippableException("환불 기한(%s)이 지난 뒤 발송할 수 있습니다.".formatted(refundDeadline));
		}
		return transactionTemplate.execute(status -> {
			OrderItem item = lockSellersItem(sellerId, orderItemId);
			item.shipKit(carrier, trackingNumber, now);
			return item.toDto();
		});
	}

	// 배송 중 → 배송 완료
	public OrderItemDto completeDelivery(int sellerId, int orderItemId, LocalDateTime now) {
		return transactionTemplate.execute(status -> {
			OrderItem item = lockSellersItem(sellerId, orderItemId);
			item.completeKitDelivery(now);
			return item.toDto();
		});
	}

	// 판매자의 키트 주문 목록. raceId·status가 null이면 거르지 않는다
	@Transactional(readOnly = true)
	public List<SellerKitDto> findKits(int sellerId, Integer raceId, KitDeliveryStatus status) {
		return orderItemRepository.findSellerKits(sellerId, raceId, status);
	}

	// 배송 상태별 건수와, 배송 준비 중 환불 기한 전이라 아직 보낼 수 없는 건수
	public SellerKitSummaryDto summarize(int sellerId, Integer raceId, LocalDateTime now) {
		Map<KitDeliveryStatus, Long> counts = new EnumMap<>(KitDeliveryStatus.class);
		for (Object[] row : orderItemRepository.countSellerKitsByStatus(sellerId, raceId)) {
			counts.put((KitDeliveryStatus)row[0], (Long)row[1]);
		}
		long beforeRefundDeadline = 0;
		for (Object[] row : orderItemRepository.countPreparingKitsByCourse(sellerId, raceId)) {
			if (!isRefundDeadlinePassed(findCourse((Integer)row[0]).refundDeadline(), now)) {
				beforeRefundDeadline += (Long)row[1];
			}
		}
		return new SellerKitSummaryDto(counts.getOrDefault(KitDeliveryStatus.PREPARING, 0L),
			counts.getOrDefault(KitDeliveryStatus.SHIPPING, 0L), counts.getOrDefault(KitDeliveryStatus.DELIVERED, 0L),
			beforeRefundDeadline);
	}

	// 환불 기한 날까지는 환불할 수 있으므로, 그다음 날부터 보낸다
	private boolean isRefundDeadlinePassed(LocalDate refundDeadline, LocalDateTime now) {
		return now.toLocalDate().isAfter(refundDeadline);
	}

	private RaceCourseInfo findCourse(int raceCourseId) {
		return raceCoursePort.findCourse(raceCourseId)
			.orElseThrow(() -> new RaceCourseNotFoundException("존재하지 않는 종목입니다."));
	}

	private OrderItem findSellersItem(int sellerId, int orderItemId) {
		Order order = orderRepository.findById(findOrderId(orderItemId)).orElseThrow();
		return checkSeller(order.findItem(orderItemId), sellerId);
	}

	// 주문을 잠그고 읽는다. 같은 참가권의 취소와 겹쳐도 한 요청씩 처리되어, 취소된 참가권은 발송하지 않는다
	private OrderItem lockSellersItem(int sellerId, int orderItemId) {
		Order order = orderRepository.findByIdForUpdate(findOrderId(orderItemId)).orElseThrow();
		return checkSeller(order.findItem(orderItemId), sellerId);
	}

	private int findOrderId(int orderItemId) {
		return orderItemRepository.findOrderIdByItemId(orderItemId)
			.orElseThrow(() -> new OrderItemNotFoundException("주문에 없는 상품입니다."));
	}

	private OrderItem checkSeller(OrderItem item, int sellerId) {
		if (!item.isSoldBy(sellerId)) {
			throw new OrderAccessDeniedException("본인 대회의 참가권만 배송 처리할 수 있습니다.");
		}
		return item;
	}
}
