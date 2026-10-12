package com.bukang.product.app;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.bukang.common.shared.file.domain.FileRef;
import com.bukang.common.shared.file.event.FileLinkRequestedEvent;
import com.bukang.common.shared.seller.domain.SellerStatus;
import com.bukang.product.domain.ProductSeller;
import com.bukang.product.domain.Race;
import com.bukang.product.domain.RaceCourse;
import com.bukang.product.domain.exception.InvalidRaceException;
import com.bukang.product.domain.exception.RaceAccessDeniedException;
import com.bukang.product.domain.exception.RaceNotFoundException;
import com.bukang.product.domain.exception.SellerNotApprovedException;
import com.bukang.product.dto.RaceCourseSaveRequestDto;
import com.bukang.product.dto.RaceImageRequestDto;
import com.bukang.product.dto.RaceSaveRequestDto;
import com.bukang.product.out.ProductSellerRepository;
import com.bukang.product.out.RaceRepository;

import lombok.RequiredArgsConstructor;

/**
 * 판매자가 본인이 등록한 대회를 등록과 같은 모양의 요청으로 통째로 바꾼다.
 * 종목은 courseId가 있으면 수정, 없으면 추가하고, 요청에 없는 기존 종목은 뺀다. 이미지 목록도 통째로 바뀐다.
 */
@Service
@RequiredArgsConstructor
public class RaceUpdateUseCase {
	private final ProductSellerRepository productSellerRepository;
	private final RaceRepository raceRepository;
	private final RaceRequestValidator raceRequestValidator;

	public RaceSaveResult update(int memberId, int raceId, RaceSaveRequestDto request) {
		ProductSeller seller = productSellerRepository.findByMemberIdAndStatus(memberId, SellerStatus.APPROVED)
			.orElseThrow(SellerNotApprovedException::new);
		Race race = raceRepository.findById(raceId).orElseThrow(RaceNotFoundException::new);
		if (!race.isOwnedBy(seller)) {
			throw new RaceAccessDeniedException();
		}
		raceRequestValidator.validate(request);

		// 판매 시작 일시가 요청 값으로 바뀌기 전에, 지금 판매가 시작됐는지 먼저 판단한다
		LocalDateTime now = LocalDateTime.now();
		boolean saleStarted = race.isSaleStarted(now);
		race.update(request, saleStarted, now);

		// 요청의 종목과 짝을 맞춘 기존 종목은 지워 나가고, 끝까지 남은 종목이 요청에 없는(뺄) 종목이다
		Map<Integer, RaceCourse> remainingCourses = new LinkedHashMap<>();
		race.getCourses().forEach(course -> remainingCourses.put(course.getId(), course));
		Map<RaceCourse, List<RaceImageRequestDto>> imagesByCourse = new LinkedHashMap<>();
		for (RaceCourseSaveRequestDto courseRequest : request.getCourses()) {
			RaceCourse course;
			if (courseRequest.getCourseId() == null) {
				course = new RaceCourse(courseRequest);
				race.addRaceCourse(course);
			} else {
				course = remainingCourses.remove(courseRequest.getCourseId());
				if (course == null) {
					throw new InvalidRaceException("이 대회의 종목이 아니거나 같은 종목을 두 번 보냈습니다. (courseId: %d)"
						.formatted(courseRequest.getCourseId()));
				}
				course.update(courseRequest, saleStarted);
			}
			imagesByCourse.put(course, courseRequest.getImages());
		}
		List<RaceCourse> removedCourses = new ArrayList<>(remainingCourses.values());
		removedCourses.forEach(course -> {
			course.checkRemovable();
			race.removeRaceCourse(course);
		});
		// 새 종목은 목록 끝에 붙었으므로 요청 순서로 다시 맞춘다
		race.sortCourses(new ArrayList<>(imagesByCourse.keySet()));
		// 새 종목 INSERT와 뺀 종목 DELETE를 실행해, 새 종목의 ID로 파일 연결 대상과 검색 문서를 만든다
		raceRepository.flush();

		List<FileLinkRequestedEvent.Ref> refs = new ArrayList<>();
		imagesByCourse.forEach((course, images) ->
			refs.add(FileLinkRequestedEvent.Ref.of(FileRef.of(course), RaceImageRequestDto.toItems(images))));
		// 뺀 종목에 붙어 있던 이미지는 빈 목록으로 보내 모두 뗀다 (file-service가 삭제 상태로 바꾼다)
		removedCourses.forEach(course -> refs.add(FileLinkRequestedEvent.Ref.of(FileRef.of(course), List.of())));

		List<Integer> removedCourseIds = removedCourses.stream().map(RaceCourse::getId).toList();
		return RaceSaveResult.of(race, removedCourseIds, new FileLinkRequestedEvent(memberId, refs));
	}
}
