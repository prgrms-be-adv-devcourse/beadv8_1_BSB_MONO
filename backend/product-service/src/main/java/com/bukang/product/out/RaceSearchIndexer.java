package com.bukang.product.out;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bukang.product.app.RaceSavedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 대회 저장이 커밋된 뒤 ES 검색 문서(race_courses)를 맞춘다
 * 롤백된 저장은 색인하지 않는다. 색인이 실패해도 DB의 대회는 그대로 두고 에러 로그만 남긴다 (나중에 다시 색인한다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RaceSearchIndexer {
	private final RaceCourseDocumentRepository raceCourseDocumentRepository;

	// 커밋 뒤 리스너의 예외는 이미 커밋된 요청의 응답을 실패로 바꾸므로 여기서 잡는다
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void index(RaceSavedEvent event) {
		try {
			raceCourseDocumentRepository.saveAll(event.documents());
			if (!event.deletedDocumentIds().isEmpty()) {
				raceCourseDocumentRepository.deleteAllById(event.deletedDocumentIds());
			}
		} catch (RuntimeException exception) {
			log.error("대회 검색 문서를 색인하지 못했습니다. 다시 색인해야 합니다. raceId={}", event.raceId(), exception);
		}
	}
}
