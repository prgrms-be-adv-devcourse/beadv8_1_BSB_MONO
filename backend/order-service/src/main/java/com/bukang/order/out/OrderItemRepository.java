package com.bukang.order.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bukang.order.domain.OrderItem;
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
}
