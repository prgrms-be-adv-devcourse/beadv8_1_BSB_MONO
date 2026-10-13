package com.bukang.payout.out;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.bukang.payout.domain.PayoutItem;

public interface PayoutItemRepository extends JpaRepository<PayoutItem, Integer> {
	// 판매자 건별 정산 조회: 응답에 정산 ID · 상태를 함께 내려주므로 정산 내역을 한 번에 읽는다 (N+1 방지)
	// 다대일(item → payout) fetch join이라 행 수가 늘지 않아 DB에서 페이지를 자를 수 있다
	// fetch join이 든 쿼리로는 개수를 셀 수 없으므로 개수 쿼리를 따로 적는다
	@Query(value = """
		select i from PayoutItem i join fetch i.payout p
		where p.sellerId = :sellerId and i.targetDate between :from and :to
		order by i.targetDate desc, i.id desc
		""",
		countQuery = """
			select count(i) from PayoutItem i join i.payout p
			where p.sellerId = :sellerId and i.targetDate between :from and :to
			""")
	Page<PayoutItem> findSellerItems(int sellerId, LocalDate from, LocalDate to, Pageable pageable);
}
