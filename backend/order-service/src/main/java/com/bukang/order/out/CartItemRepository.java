package com.bukang.order.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bukang.order.domain.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
	/**
	 * 장바구니 결제로 주문한 종목을 그 회원의 장바구니에서 뺀다 (PRO-45)
	 * 엔티티로 지우면 회원이 같은 항목을 동시에 지웠을 때 0행 삭제 오류로 주문까지 롤백되므로, 조건부 DELETE로 지운다.
	 * 이미 없으면 0을 돌려줄 뿐이다
	 */
	@Modifying(flushAutomatically = true)
	@Query("""
		delete from CartItem i
		where i.raceCourseId = :raceCourseId
			and i.cart.id in (select c.id from Cart c where c.memberId = :memberId)
		""")
	int deleteByMemberIdAndRaceCourseId(@Param("memberId") int memberId, @Param("raceCourseId") int raceCourseId);
}
