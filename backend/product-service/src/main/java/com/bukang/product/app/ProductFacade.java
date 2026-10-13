package com.bukang.product.app;

import java.util.Optional;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.bukang.common.global.json.JsonConverter;
import com.bukang.common.shared.kafka.KafkaTopics;
import com.bukang.common.shared.member.event.MemberJoinedEvent;
import com.bukang.common.shared.seller.event.SellerChangedEvent;
import com.bukang.product.domain.ProductMember;
import com.bukang.product.domain.ProductSeller;
import com.bukang.product.dto.RaceSaveDto;
import com.bukang.product.dto.RaceSaveRequestDto;
import com.bukang.product.out.RaceCourseDocumentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductFacade {
	private final ProductSyncMemberUseCase productSyncMemberUseCase;
	private final ProductSyncSellerUseCase productSyncSellerUseCase;
	private final RaceRegisterUseCase raceRegisterUseCase;
	private final RaceUpdateUseCase raceUpdateUseCase;
	private final RaceSaleStartUseCase raceSaleStartUseCase;
	private final TransactionTemplate transactionTemplate;
	private final RaceCourseDocumentRepository raceCourseDocumentRepository;
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final JsonConverter jsonConverter;

	@Transactional
	public ProductMember syncMember(MemberJoinedEvent event) {
		return productSyncMemberUseCase.syncMember(event);
	}

	@Transactional
	public Optional<ProductSeller> syncSeller(SellerChangedEvent event) {
		return productSyncSellerUseCase.syncSeller(event);
	}

	// 대회 저장(등록·수정·판매 시작)은 DB에 커밋한 뒤에 검색 색인과 파일 연결 메시지 발행을 한다
	// 롤백된 저장을 색인하거나, 롤백된 대회에 파일을 연결하지 않기 위해서다
	// NOT_SUPPORTED: 이 메서드는 트랜잭션 없이 실행한다 (클래스에 붙은 readOnly 트랜잭션을 쓰지 않는다)
	// transactionTemplate.execute(): 넘긴 람다만 새 트랜잭션으로 실행하고, execute()가 값을 돌려주기 전에 커밋한다
	// 그래서 다음 줄로 넘어온 시점에는 저장이 이미 커밋되어 있고, 저장이 실패하면 예외가 나서 다음 줄로 오지 않는다
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public RaceSaveDto registerRace(int memberId, RaceSaveRequestDto request) {
		RaceSaveResult result = transactionTemplate.execute(status ->
			raceRegisterUseCase.register(memberId, request));
		indexRaceCourses(result);
		publishFileLink(result);
		return result.dto();
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public RaceSaveDto updateRace(int memberId, int raceId, RaceSaveRequestDto request) {
		RaceSaveResult result = transactionTemplate.execute(status ->
			raceUpdateUseCase.update(memberId, raceId, request));
		indexRaceCourses(result);
		publishFileLink(result);
		return result.dto();
	}

	// 이미지는 바뀌지 않으므로 검색 문서(판매 상태)만 다시 색인한다
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public RaceSaveDto startRaceSale(int memberId, int raceId) {
		RaceSaveResult result = transactionTemplate.execute(status ->
			raceSaleStartUseCase.startSale(memberId, raceId));
		indexRaceCourses(result);
		return result.dto();
	}

	// ES 검색 문서(race_courses)를 맞춘다
	// 이미 커밋된 뒤라 실패해도 대회를 되돌릴 수 없으므로, 예외를 잡아 에러 로그만 남긴다 (나중에 다시 색인한다)
	// 잡지 않으면 저장은 됐는데 응답이 실패(500)로 나간다
	private void indexRaceCourses(RaceSaveResult result) {
		try {
			raceCourseDocumentRepository.saveAll(result.documents());
			if (!result.deletedDocumentIds().isEmpty()) {
				raceCourseDocumentRepository.deleteAllById(result.deletedDocumentIds());
			}
		} catch (RuntimeException exception) {
			log.error("대회 검색 문서를 색인하지 못했습니다. 다시 색인해야 합니다. raceId={}", result.raceId(), exception);
		}
	}

	// file-service에 이미지 연결 메시지를 보낸다 (race.filelink.requested)
	// 같은 대회의 메시지가 순서대로 처리되도록 대회 ID를 key로 보낸다 (같은 key는 같은 파티션)
	// 보내지 못하면 이미 커밋된 대회를 되돌릴 수 없어 에러 로그에 메시지를 남긴다
	// 연결되지 않은 파일은 하루 뒤 file-service 정리 배치가 지우므로, 그 전에 로그의 메시지를 다시 보내야 한다
	private void publishFileLink(RaceSaveResult result) {
		String key = String.valueOf(result.raceId());
		String payload = jsonConverter.toJson(result.fileLink());
		try {
			kafkaTemplate.send(KafkaTopics.RACE_FILE_LINK_REQUESTED, key, payload)
				.whenComplete((sendResult, exception) -> {
					if (exception != null) {
						log.error("파일 연결 메시지를 보내지 못했습니다. raceId={}, payload={}", key, payload, exception);
					}
				});
		} catch (RuntimeException exception) {
			// 브로커에 연결하지 못하면 send()가 기다리다(max.block.ms, 기본 60초) 예외를 던진다
			log.error("파일 연결 메시지를 보내지 못했습니다. raceId={}, payload={}", key, payload, exception);
		}
	}
}
