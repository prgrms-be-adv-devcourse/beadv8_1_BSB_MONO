package com.bukang.product.app;

import java.util.List;

import com.bukang.common.shared.file.event.FileLinkRequestedEvent;
import com.bukang.product.domain.Race;
import com.bukang.product.domain.RaceCourse;
import com.bukang.product.domain.document.RaceCourseDocument;
import com.bukang.product.dto.RaceSaveDto;

/**
 * 대회 저장(등록·수정·취소) 유스케이스의 결과. ProductFacade가 커밋한 뒤 검색 색인과 파일 연결 메시지 발행에 쓴다.
 * 커밋 뒤에는 엔티티의 지연 로딩(race.getCourses())을 쓸 수 없으므로, 필요한 값은 트랜잭션 안에서 미리 만들어 담는다.
 */
public record RaceSaveResult(
	// API 응답
	RaceSaveDto dto,
	// 다시 색인할 종목 문서 (대회의 모든 종목)
	List<RaceCourseDocument> documents,
	// 수정 때 뺀 종목의 문서 ID
	List<String> deletedDocumentIds,
	// 이미지 연결 메시지. 이미지를 바꾸지 않는 저장(취소)이면 null
	FileLinkRequestedEvent fileLink
) {
	public static RaceSaveResult of(Race race, List<Integer> deletedCourseIds, FileLinkRequestedEvent fileLink) {
		List<RaceCourse> courses = race.getCourses();
		RaceSaveDto dto = new RaceSaveDto(
			race.getId(),
			race.getSaleStatus(),
			race.getSaleOpenAt(),
			courses.stream().map(RaceCourse::getId).toList()
		);
		return new RaceSaveResult(
			dto,
			courses.stream().map(RaceCourseDocument::from).toList(),
			deletedCourseIds.stream().map(String::valueOf).toList(),
			fileLink
		);
	}

	public int raceId() {
		return dto.getRaceId();
	}
}
