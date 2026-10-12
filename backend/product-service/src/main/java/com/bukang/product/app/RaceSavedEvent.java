package com.bukang.product.app;

import java.util.List;

import com.bukang.common.shared.file.event.FileLinkRequestedEvent;
import com.bukang.product.domain.Race;
import com.bukang.product.domain.document.RaceCourseDocument;

/**
 * 대회를 저장(등록·수정·판매 시작)했을 때 서비스 안에서 발행하는 이벤트.
 * 커밋된 뒤에 RaceSearchIndexer가 검색 문서를 색인하고, FileLinkPublisher가 파일 연결 메시지를 보낸다.
 * 커밋 뒤에는 엔티티의 지연 로딩을 쓸 수 없으므로, 필요한 값은 트랜잭션 안에서 미리 만들어 담는다.
 */
public record RaceSavedEvent(
	int raceId,
	// 다시 색인할 종목 문서 (대회의 모든 종목)
	List<RaceCourseDocument> documents,
	// 수정 때 뺀 종목의 문서 ID
	List<String> deletedDocumentIds,
	// 이미지 연결 메시지. 이미지를 바꾸지 않는 저장(판매 시작)이면 null
	FileLinkRequestedEvent fileLink
) {
	public static RaceSavedEvent of(Race race, List<Integer> deletedCourseIds, FileLinkRequestedEvent fileLink) {
		return new RaceSavedEvent(
			race.getId(),
			race.getCourses().stream().map(RaceCourseDocument::from).toList(),
			deletedCourseIds.stream().map(String::valueOf).toList(),
			fileLink
		);
	}
}
