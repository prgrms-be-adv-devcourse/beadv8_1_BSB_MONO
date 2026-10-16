package com.bukang.member.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bukang.common.shared.seller.domain.SellerStatus;
import com.bukang.member.domain.Seller;

import jakarta.persistence.LockModeType;

public interface SellerRepository extends JpaRepository<Seller, Integer> {
	// 대기·승인 상태 신청이 있으면 다시 신청할 수 없다 (idx_member_seller_member)
	boolean existsByMemberIdAndStatusIn(int memberId, Collection<SellerStatus> statuses);

	// 가장 최근 신청. ID가 신청 순서대로 늘어난다
	Optional<Seller> findFirstByMemberIdOrderByIdDesc(int memberId);

	List<Seller> findAllByOrderByIdDesc();

	List<Seller> findAllByStatusOrderByIdDesc(SellerStatus status);

	// 심사할 신청 줄을 잠그고 읽는다. 관리자 두 명이 같은 신청을 동시에 심사하면 뒤 요청은 앞 요청이 끝난 뒤
	// 바뀐 상태(APPROVED·REJECTED)를 읽어 409가 된다 (이벤트가 두 번 나가지 않게)
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from Seller s where s.id = :id")
	Optional<Seller> findByIdForUpdate(@Param("id") int id);
}
