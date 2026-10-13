package com.bukang.product.out;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bukang.product.domain.ProductSeller;
import com.bukang.product.domain.Race;
import com.bukang.product.domain.RaceSaleStatus;

import jakarta.persistence.LockModeType;

public interface RaceRepository extends JpaRepository<Race, Integer> {
	// 판매자의 대회 (최근 등록 순)
	List<Race> findAllBySellerOrderByIdDesc(ProductSeller seller);

	// 대회 행을 공유 잠금으로 읽는다 (MySQL SELECT … FOR SHARE). 자리 확보가 판매 상태를 볼 때 쓴다
	// 확보끼리는 서로 막지 않고, 대회 행을 바꾸는 요청이 있으면 커밋을 기다렸다가 최신 상태를 본다
	@Lock(LockModeType.PESSIMISTIC_READ)
	@Query("select r from Race r where r.id = :id")
	Optional<Race> findByIdForShare(@Param("id") int id);

	// 판매 상태 배치가 바꿀 대회 (판매 예정인데 판매 시작 일시가 지난 대회)
	@Query("select r.id from Race r where r.saleStatus = :status and r.saleOpenAt <= :now")
	List<Integer> findIdsToOpen(@Param("status") RaceSaleStatus status, @Param("now") LocalDateTime now);

	// 판매 상태 배치가 바꿀 대회 (대회일이 지난 판매 예정·판매 중 대회)
	@Query("select r.id from Race r where r.saleStatus in :statuses and r.raceDate < :before")
	List<Integer> findIdsToClose(@Param("statuses") Collection<RaceSaleStatus> statuses,
		@Param("before") LocalDateTime before);

	// 아래 두 UPDATE는 ID를 읽을 때의 조건을 UPDATE에서 한 번 더 확인한다 (조건부 UPDATE)
	// ID를 읽은 뒤 판매자가 대회일·판매 시작 일시를 미뤘거나 상태를 바꿨으면 그 대회는 건너뛴다
	// 엔티티를 거치지 않으므로 수정 일시(updated_at)도 직접 채운다

	// 대회일이 지난 판매 예정·판매 중 대회 → 판매 종료
	@Modifying(clearAutomatically = true)
	@Query("update Race r set r.saleStatus = com.bukang.product.domain.RaceSaleStatus.CLOSED, r.modifyDate = :now "
		+ "where r.id in :ids and r.saleStatus in :statuses and r.raceDate < :before")
	int closeSale(@Param("ids") Collection<Integer> ids, @Param("statuses") Collection<RaceSaleStatus> statuses,
		@Param("before") LocalDateTime before, @Param("now") LocalDateTime now);

	// 판매 시작 일시가 지난 판매 예정 대회 → 판매 중
	@Modifying(clearAutomatically = true)
	@Query("update Race r set r.saleStatus = com.bukang.product.domain.RaceSaleStatus.ON_SALE, r.modifyDate = :now "
		+ "where r.id in :ids and r.saleStatus = com.bukang.product.domain.RaceSaleStatus.SCHEDULED "
		+ "and r.saleOpenAt <= :now")
	int openSale(@Param("ids") Collection<Integer> ids, @Param("now") LocalDateTime now);
}
