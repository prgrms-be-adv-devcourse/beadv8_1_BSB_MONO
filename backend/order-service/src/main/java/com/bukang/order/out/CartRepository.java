package com.bukang.order.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bukang.order.domain.Cart;

import jakarta.persistence.LockModeType;

public interface CartRepository extends JpaRepository<Cart, Integer> {
	Optional<Cart> findByMemberId(int memberId);

	// 담기를 한 요청씩 처리해, 같은 회원이 동시에 다른 종목을 담아도 한 종목만 남게 한다 (PRO-192)
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from Cart c where c.memberId = :memberId")
	Optional<Cart> findByMemberIdForUpdate(@Param("memberId") int memberId);
}
