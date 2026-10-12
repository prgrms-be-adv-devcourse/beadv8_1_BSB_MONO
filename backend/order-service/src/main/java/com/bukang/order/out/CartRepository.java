package com.bukang.order.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bukang.order.domain.Cart;

public interface CartRepository extends JpaRepository<Cart, Integer> {
	Optional<Cart> findByMemberId(int memberId);
}
