package com.bukang.order.out;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bukang.order.domain.Order;

import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<Order, Integer> {
	// 같은 Idempotency-Key로 이미 만든 주문 (uk_order_idempotency와 같은 기준)
	Optional<Order> findByBuyerIdAndIdempotencyKey(int buyerId, String idempotencyKey);

	// 결제 기한 늘리기가 같은 주문에 동시에 들어오면 한 요청씩 처리한다 (한 번만 늘리기)
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from Order o where o.id = :id")
	Optional<Order> findByIdForUpdate(@Param("id") int id);

	// 결제 기한이 지난 결제 대기 주문을 만료(EXPIRED)로 끝낸다. 조건부 UPDATE라 여러 번 돌아도 결과가 같다 (PRO-42)
	// 일괄 UPDATE는 JPA 감사(@LastModifiedDate)를 거치지 않으므로 수정 일시도 직접 넣는다
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
		update Order o
		set o.status = com.bukang.order.domain.OrderStatus.EXPIRED, o.modifyDate = :now
		where o.status = com.bukang.order.domain.OrderStatus.CREATED and o.paymentDueAt <= :now
		""")
	int expireOverdue(@Param("now") LocalDateTime now);
}
