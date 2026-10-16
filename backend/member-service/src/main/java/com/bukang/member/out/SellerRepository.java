package com.bukang.member.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bukang.common.shared.seller.domain.SellerStatus;
import com.bukang.member.domain.Seller;

public interface SellerRepository extends JpaRepository<Seller, Integer> {
	// 대기·승인 상태 신청이 있으면 다시 신청할 수 없다 (idx_member_seller_member)
	boolean existsByMemberIdAndStatusIn(int memberId, Collection<SellerStatus> statuses);

	// 가장 최근 신청. ID가 신청 순서대로 늘어난다
	Optional<Seller> findFirstByMemberIdOrderByIdDesc(int memberId);

	List<Seller> findAllByOrderByIdDesc();

	List<Seller> findAllByStatusOrderByIdDesc(SellerStatus status);
}
