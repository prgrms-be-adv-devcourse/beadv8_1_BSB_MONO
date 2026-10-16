package com.bukang.member.app;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.common.shared.seller.domain.SellerStatus;
import com.bukang.member.domain.Seller;
import com.bukang.member.domain.exception.SellerNotFoundException;
import com.bukang.member.dto.SellerApplyRequestDto;
import com.bukang.member.dto.SellerDto;
import com.bukang.member.out.SellerRepository;

import lombok.RequiredArgsConstructor;

// 판매자 등록 신청과 관리자 심사 (BE-020)
// 신청·승인·반려는 트랜잭션 안에서 이벤트를 발행해야 커밋 뒤 Kafka로 나간다 (MemberKafkaPublisher)
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SellerFacade {
	private final SellerApplyUseCase sellerApplyUseCase;
	private final SellerReviewUseCase sellerReviewUseCase;
	private final AdminVerifyUseCase adminVerifyUseCase;
	private final SellerRepository sellerRepository;

	@Transactional
	public SellerDto apply(int memberId, SellerApplyRequestDto request) {
		return SellerDto.from(sellerApplyUseCase.apply(
			memberId,
			request.getSellerType(),
			request.getBusinessName(),
			request.getBusinessNumber(),
			request.getRepresentativeName(),
			LocalDateTime.now()
		));
	}

	public SellerDto findMine(int memberId) {
		return sellerRepository.findFirstByMemberIdOrderByIdDesc(memberId)
			.map(SellerDto::from)
			.orElseThrow(() -> new SellerNotFoundException("판매자 신청 내역이 없습니다."));
	}

	// status가 null이면 전체를 최근 신청부터 돌려준다
	public List<SellerDto> findAllForAdmin(int adminId, SellerStatus status) {
		adminVerifyUseCase.verify(adminId);
		List<Seller> sellers = status == null
			? sellerRepository.findAllByOrderByIdDesc()
			: sellerRepository.findAllByStatusOrderByIdDesc(status);
		return sellers.stream().map(SellerDto::from).toList();
	}

	@Transactional
	public SellerDto approve(int adminId, int sellerId) {
		adminVerifyUseCase.verify(adminId);
		return SellerDto.from(sellerReviewUseCase.approve(adminId, sellerId, LocalDateTime.now()));
	}

	@Transactional
	public SellerDto reject(int adminId, int sellerId, String reason) {
		adminVerifyUseCase.verify(adminId);
		return SellerDto.from(sellerReviewUseCase.reject(adminId, sellerId, reason, LocalDateTime.now()));
	}
}
