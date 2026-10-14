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

	// 배송지를 적은 최근 신청. 마지막 신청이 키트 없는 종목이라 배송지가 비어 있을 때 다음 신청서에 채울 배송지를 찾는다
	Optional<Participation> findFirstByMemberIdAndParticipantShippingAddressAddressIsNotNullOrderByIdDesc(int memberId);

	// [결제하기]가 같은 신청서로 동시에 들어오면 한 요청씩 처리해, 상품 서비스에 자리를 두 번 잡지 않게 한다
	// 결제·연장·참가자 정보 수정도 이걸로 읽어, 서로 겹쳐도 한쪽이 바꾼 상태를 다른 쪽이 옛 값으로 덮지 않게 한다
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Participation p where p.id = :id")
	Optional<Participation> findByIdForUpdate(@Param("id") int id);
}
