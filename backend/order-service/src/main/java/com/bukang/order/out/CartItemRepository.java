package com.bukang.order.out;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bukang.order.domain.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
}
