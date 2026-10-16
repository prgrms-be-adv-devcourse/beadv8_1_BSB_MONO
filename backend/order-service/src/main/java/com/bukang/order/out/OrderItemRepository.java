package com.bukang.order.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bukang.order.domain.KitDeliveryStatus;
import com.bukang.order.domain.OrderItem;
import com.bukang.order.dto.SellerKitDto;
import com.bukang.order.dto.SoldEntryDto;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
	/**
	 * 대회 일시가 [from, to)인 주문 상품 중 결제 완료(PAID)인 것. 취소(CANCELED)·결제 전(ORDERED)은 빠진다
	 * 정산 배치가 정산 대상일에 부르므로, 그 전에 취소된 참가권은 돌려주지 않는다 (정산 전 취소)
	 */
	@Query("""
		select new com.bukang.order.dto.SoldEntryDto(i.id, i.sellerId, i.raceId, i.raceDate, i.amount)
		from OrderItem i
		where i.raceDate >= :from and i.raceDate < :to
			and i.status = com.bukang.order.domain.OrderItemStatus.PAID
		order by i.id
		""")
	List<SoldEntryDto> findSoldEntries(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	// 참가 신청으로 만든 주문의 ID. 참가 신청 하나에는 주문 상품이 하나뿐이다 (uk_order_item_participation)
	@Query("select i.order.id from OrderItem i where i.participationId = :participationId")
	Optional<Integer> findOrderIdByParticipationId(@Param("participationId") int participationId);

	// 주문 상품이 속한 주문의 ID. 주문을 잠그고 바꾸기 전에 읽는다 (키트 발송, PRO-136)
	@Query("select i.order.id from OrderItem i where i.id = :orderItemId")
	Optional<Integer> findOrderIdByItemId(@Param("orderItemId") int orderItemId);

	/**
	 * 판매자의 키트 주문 목록 (PRO-136). 키트가 있는 참가권 중 결제했거나 이미 보낸 것만, 대회·배송 상태로 거른다(null이면 거르지 않음)
	 * 보낸 뒤 취소된 참가권(대회 취소 환불 등)도 키트는 가고 있으므로 남겨, 판매자가 배송 완료까지 바꿀 수 있게 한다
	 * 배송지는 주문의 배송지(구매자가 바꾼 값)를, 참가자 이름·티셔츠 사이즈는 참가 신청의 값을 쓴다
	 */
	@Query(value = """
		select new com.bukang.order.dto.SellerKitDto(i.id, o.orderNo, i.status, i.raceId, i.raceCourseId, i.itemName,
			p.participant.name, p.participant.tshirtSize, o.shippingAddress,
			i.kitDeliveryStatus, i.kitCarrier, i.kitTrackingNumber, i.kitShippedAt, i.kitDeliveredAt)
		from OrderItem i
			join i.order o
			join Participation p on p.id = i.participationId
		where i.sellerId = :sellerId
			and i.kitDeliveryStatus is not null
			and (i.status = com.bukang.order.domain.OrderItemStatus.PAID
				or i.kitDeliveryStatus <> com.bukang.order.domain.KitDeliveryStatus.PREPARING)
			and (:raceId is null or i.raceId = :raceId)
			and (:status is null or i.kitDeliveryStatus = :status)
		order by i.id desc
		""", countQuery = """
		select count(i)
		from OrderItem i
			join Participation p on p.id = i.participationId
		where i.sellerId = :sellerId
			and i.kitDeliveryStatus is not null
			and (i.status = com.bukang.order.domain.OrderItemStatus.PAID
				or i.kitDeliveryStatus <> com.bukang.order.domain.KitDeliveryStatus.PREPARING)
			and (:raceId is null or i.raceId = :raceId)
			and (:status is null or i.kitDeliveryStatus = :status)
		""")
	Page<SellerKitDto> findSellerKits(@Param("sellerId") int sellerId, @Param("raceId") Integer raceId,
		@Param("status") KitDeliveryStatus status, Pageable pageable);

	// 판매자의 키트 주문 배송 상태별 건수. [상태, 건수] 줄로 돌려준다 (findSellerKits와 같은 조건)
	@Query("""
		select i.kitDeliveryStatus, count(i)
		from OrderItem i
		where i.sellerId = :sellerId
			and i.kitDeliveryStatus is not null
			and (i.status = com.bukang.order.domain.OrderItemStatus.PAID
				or i.kitDeliveryStatus <> com.bukang.order.domain.KitDeliveryStatus.PREPARING)
			and (:raceId is null or i.raceId = :raceId)
		group by i.kitDeliveryStatus
		""")
	List<Object[]> countSellerKitsByStatus(@Param("sellerId") int sellerId, @Param("raceId") Integer raceId);

	// 배송 준비인 키트 주문의 종목별 건수. [종목 ID, 건수] 줄로 돌려준다. 종목마다 환불 기한이 지났는지 따로 본다
	@Query("""
		select i.raceCourseId, count(i)
		from OrderItem i
		where i.sellerId = :sellerId
			and i.status = com.bukang.order.domain.OrderItemStatus.PAID
			and i.kitDeliveryStatus = com.bukang.order.domain.KitDeliveryStatus.PREPARING
			and (:raceId is null or i.raceId = :raceId)
		group by i.raceCourseId
		""")
	List<Object[]> countPreparingKitsByCourse(@Param("sellerId") int sellerId, @Param("raceId") Integer raceId);
}
