package com.bukang.order.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bukang.order.domain.Order;

public interface OrderRepository extends JpaRepository<Order, Integer> {
	// 같은 Idempotency-Key로 이미 만든 주문 (uk_order_idempotency와 같은 기준)
	Optional<Order> findByBuyerIdAndIdempotencyKey(int buyerId, String idempotencyKey);
}
