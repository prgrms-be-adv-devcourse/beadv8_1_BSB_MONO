package com.bukang.product.in;

import java.time.LocalDateTime;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.common.shared.seller.domain.SellerStatus;
import com.bukang.common.shared.seller.domain.SellerType;
import com.bukang.common.shared.seller.event.SellerChangedEvent;
import com.bukang.product.app.ProductFacade;
import com.bukang.product.out.ProductMemberRepository;
import com.bukang.product.out.ProductSellerRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 개발용 기본 회원·판매자 복제본
 * 상품 서비스는 회원·판매자를 Kafka 메시지(queuing.member.joined, queuing.seller.changed)로 받아 복제본에 저장한다.
 * dev DB를 비우면 복제본도 비어 대회를 등록할 수 없으므로(403), 메시지를 받은 것처럼 같은 저장 경로
 * (ProductFacade.syncMember·syncSeller)로 넣는다.
 * 회원 ID는 member-service MemberDataInit의 가입 순서(system 1, holding 2, 관리자 3, 사용자1~3 4~6, 판매자1 7)를 따른다.
 * member-service가 같은 회원의 메시지를 보내도 같은 ID로 덮어쓰므로 결과가 같다
 * (수정 시각이 여기서 넣은 값보다 이르면 건너뛰지만 닉네임이 같다).
 * dev 프로파일에서만 실행한다 (prod, test에서는 만들지 않음)
 */
@Profile("dev")
@Configuration
@Slf4j
public class ProductDataInit {
	// 판매자1 (회원 7). 판매자 ID는 payout-service PayoutDataInit의 정산 샘플 판매자 ID와 같게 둔다
	// 더미 페이지(race-register-test.html, race-query-test.html)의 기본 회원 ID·판매자 ID도 이 값이다
	private static final int SELLER_MEMBER_ID = 7;
	private static final int SELLER_ID = 7;

	private final ProductDataInit self;
	private final ProductFacade productFacade;
	private final ProductMemberRepository productMemberRepository;
	private final ProductSellerRepository productSellerRepository;

	public ProductDataInit(
		@Lazy ProductDataInit self,
		ProductFacade productFacade,
		ProductMemberRepository productMemberRepository,
		ProductSellerRepository productSellerRepository
	) {
		this.self = self;
		this.productFacade = productFacade;
		this.productMemberRepository = productMemberRepository;
		this.productSellerRepository = productSellerRepository;
	}

	@Bean
	@Order(1)
	public ApplicationRunner productDataInitApplicationRunner() {
		return args -> self.makeBaseMembersAndSellers();
	}

	@Transactional
	public void makeBaseMembersAndSellers() {
		// dev DB는 재시작해도 데이터가 남으므로(ddl-auto: update) 이미 있는 복제본은 건너뛴다
		member(1, "system");
		member(2, "holding");
		member(3, "관리자");
		member(4, "사용자1");
		member(5, "사용자2");
		member(6, "사용자3");
		member(SELLER_MEMBER_ID, "판매자1");
		// 회원 복제본이 있어야 판매자를 저장하므로 회원 다음에 만든다
		approvedSeller(SELLER_ID, SELLER_MEMBER_ID, "크루런 스포츠", "123-45-67890", "판매자1");
	}

	private void member(int id, String nickname) {
		if (productMemberRepository.existsById(id)) {
			return;
		}

		LocalDateTime now = LocalDateTime.now();
		productFacade.syncMember(id, now, now, nickname);
		log.info("기본 회원 복제본 생성: id={}, nickname={}", id, nickname);
	}

	private void approvedSeller(int id, int memberId, String businessName, String businessNumber,
		String representativeName) {
		if (productSellerRepository.existsById(id)) {
			return;
		}

		LocalDateTime now = LocalDateTime.now();
		productFacade.syncSeller(new SellerChangedEvent(id, memberId, SellerType.BUSINESS, businessName,
			businessNumber, representativeName, SellerStatus.APPROVED, now, now));
		log.info("기본 판매자 복제본 생성: sellerId={}, memberId={}", id, memberId);
	}
}
