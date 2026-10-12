package com.bukang.order.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bukang.order.domain.Participation;

public interface ParticipationRepository extends JpaRepository<Participation, Integer> {
	// 같은 대회에 살아 있는 신청이 있는지 (uk_participation_active와 같은 기준)
	boolean existsByRaceIdAndMemberIdAndActiveFlagTrue(int raceId, int memberId);

	// 다음 신청 때 채울 최근 신청 (idx_participation_member 인덱스). 상태와 상관없이 마지막으로 쓴 정보를 쓴다
	Optional<Participation> findFirstByMemberIdOrderByIdDesc(int memberId);
}
