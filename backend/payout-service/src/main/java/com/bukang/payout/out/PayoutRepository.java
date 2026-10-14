package com.bukang.payout.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.bukang.payout.domain.Payout;
import com.bukang.payout.domain.PayoutStatus;

public interface PayoutRepository extends JpaRepository<Payout, Integer> {
	boolean existsBySellerIdAndRaceIdAndPayoutMonth(int sellerId, int raceId, String payoutMonth);

	// 지급 배치: 지급 예정일이 오늘이거나 지난 정산 내역 (지난 날을 놓쳐도 다음 실행에서 따라잡는다)
	List<Payout> findByStatusAndScheduledPayDateLessThanEqual(PayoutStatus status, LocalDate date);

	// 판매자 정산 내역 조회: 정산 월(YYYY-MM)은 문자열이어도 자릿수가 같아 사전순 비교가 달 순서와 같다
	// 유일 조건 (seller_id, race_id, payout_month)의 인덱스는 seller_id로 시작하므로 판매자 범위를 이 인덱스로 찾는다
	List<Payout> findBySellerIdAndPayoutMonthBetweenOrderByPayoutMonthDescIdDesc(
		int sellerId, String fromMonth, String toMonth);

	// 판매자 요약: 지급 예정인 정산 내역 중 가장 이른 지급 예정일 (다음 정산일)
	Optional<Payout> findFirstBySellerIdAndStatusOrderByScheduledPayDateAsc(int sellerId, PayoutStatus status);

	// 판매자 요약: 그 지급일에 나갈 지급액 합계 (정산 예정액). 보류(HELD)된 정산 내역은 상태가 달라 빠진다
	@Query("""
		select coalesce(sum(p.payoutAmount), 0) from Payout p
		where p.sellerId = :sellerId and p.status = :status and p.scheduledPayDate = :payDate
		""")
	long sumPayoutAmount(int sellerId, PayoutStatus status, LocalDate payDate);

	// 판매자 요약: 지급보류 중인 정산 내역 수. 한 정산 내역에는 활성 보류가 하나뿐이라 보류 건수와 같다
	long countBySellerIdAndStatus(int sellerId, PayoutStatus status);
}
