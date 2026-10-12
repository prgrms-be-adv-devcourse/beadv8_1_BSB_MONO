package com.bukang.order.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bukang.order.domain.Participation;

import jakarta.persistence.LockModeType;

public interface ParticipationRepository extends JpaRepository<Participation, Integer> {
	// 같은 대회에 살아 있는 신청이 있는지 (uk_participation_active와 같은 기준)
	boolean existsByRaceIdAndMemberIdAndActiveFlagTrue(int raceId, int memberId);

	// 다음 신청 때 채울 최근 신청 (idx_participation_member 인덱스). 상태와 상관없이 마지막으로 쓴 정보를 쓴다
	Optional<Participation> findFirstByMemberIdOrderByIdDesc(int memberId);

	// [결제하기]가 같은 신청서로 동시에 들어오면 한 요청씩 처리해, 상품 서비스에 자리를 두 번 잡지 않게 한다
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Participation p where p.id = :id")
	Optional<Participation> findByIdForUpdate(@Param("id") int id);
}
