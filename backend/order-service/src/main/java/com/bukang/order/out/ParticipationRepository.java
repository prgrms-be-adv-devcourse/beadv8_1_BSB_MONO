package com.bukang.order.out;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bukang.order.domain.Participation;

import jakarta.persistence.LockModeType;

public interface ParticipationRepository extends JpaRepository<Participation, Integer> {
	// 같은 대회에 살아 있는 신청이 있는지 (uk_participation_active와 같은 기준)
	boolean existsByRaceIdAndMemberIdAndActiveFlagTrue(int raceId, int memberId);

	// 다음 신청 때 채울 최근 신청 (idx_participation_member 인덱스). 상태와 상관없이 마지막으로 쓴 정보를 쓴다
	Optional<Participation> findFirstByMemberIdOrderByIdDesc(int memberId);

	// 배송 주소를 적은 최근 신청. 마지막 신청이 키트 없는 종목이라 주소가 비어 있을 때 다음 신청서에 채울 주소를 찾는다
	Optional<Participation> findFirstByMemberIdAndParticipantShippingAddressIsNotNullOrderByIdDesc(int memberId);

	// [결제하기]가 같은 신청서로 동시에 들어오면 한 요청씩 처리해, 상품 서비스에 자리를 두 번 잡지 않게 한다
	// 결제·연장·참가자 정보 수정도 이걸로 읽어, 서로 겹쳐도 한쪽이 바꾼 상태를 다른 쪽이 옛 값으로 덮지 않게 한다
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Participation p where p.id = :id")
	Optional<Participation> findByIdForUpdate(@Param("id") int id);

	/**
	 * 결제 기한이 지난 결제 대기 신청을 미결제 해제(RELEASED)로 끝낸다 (만료 정리 배치, PRO-42)
	 * 결제와 겹쳐도 한쪽만 반영되도록 "지금도 APPLIED인 행"만 바꾸는 조건부 UPDATE다. 여러 번 돌아도 결과가 같다
	 * active_flag도 함께 비워 같은 대회에 다시 신청할 수 있게 한다 (Participation.changeStatus와 같은 규칙)
	 * 일괄 UPDATE는 JPA 감사(@LastModifiedDate)를 거치지 않으므로 수정 일시도 직접 넣는다
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
		update Participation p
		set p.status = com.bukang.order.domain.ParticipationStatus.RELEASED, p.activeFlag = null, p.releasedAt = :now,
			p.modifyDate = :now
		where p.status = com.bukang.order.domain.ParticipationStatus.APPLIED and p.paymentDueAt <= :now
		""")
	int releaseOverdue(@Param("now") LocalDateTime now);
}
