package com.bukang.product.app;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.bukang.common.shared.file.domain.FileRef;
import com.bukang.common.shared.file.event.FileLinkRequestedEvent;
import com.bukang.common.shared.seller.domain.SellerStatus;
import com.bukang.product.domain.ProductSeller;
import com.bukang.product.domain.Race;
import com.bukang.product.domain.RaceCourse;
import com.bukang.product.domain.exception.SellerNotApprovedException;
import com.bukang.product.dto.RaceCourseSaveRequestDto;
import com.bukang.product.dto.RaceImageRequestDto;
import com.bukang.product.dto.RaceSaveRequestDto;
import com.bukang.product.out.ProductSellerRepository;
import com.bukang.product.out.RaceRepository;

import lombok.RequiredArgsConstructor;

/**
 * 승인된 판매자가 대회와 종목을 작성 중(DRAFT)으로 등록한다.
 * 이미지는 커밋된 뒤 Kafka 메시지로 file-service에 연결을 요청한다 (대회 이미지는 Race, 종목 이미지는 RaceCourse 대상).
 */
@Service
@RequiredArgsConstructor
public class RaceRegisterUseCase {
	private final ProductSellerRepository productSellerRepository;
	private final RaceRepository raceRepository;
	private final RaceRequestValidator raceRequestValidator;
	private final ApplicationEventPublisher eventPublisher;

	public Race register(int memberId, RaceSaveRequestDto request) {
		// 회원당 승인된 판매자는 1명이라고 가정한다 (ProductSellerRepository)
		ProductSeller seller = productSellerRepository.findByMemberIdAndStatus(memberId, SellerStatus.APPROVED)
			.orElseThrow(SellerNotApprovedException::new);
		raceRequestValidator.validate(request);

		Race race = new Race(seller, request);
		// 종목마다 요청의 종목 이미지를 함께 기억해 두었다가, 저장해 ID가 생긴 뒤 연결 대상을 만든다
		Map<RaceCourse, List<RaceImageRequestDto>> imagesByCourse = new LinkedHashMap<>();
		for (RaceCourseSaveRequestDto courseRequest : request.getCourses()) {
			RaceCourse course = new RaceCourse(courseRequest);
			race.addRaceCourse(course);
			imagesByCourse.put(course, courseRequest.getImages());
		}
		// 종목도 함께 저장된다 (cascade PERSIST). ID 생성 방식이 IDENTITY라 저장하면 바로 INSERT되어 ID가 생긴다
		raceRepository.save(race);

		List<FileLinkRequestedEvent.Ref> refs = new ArrayList<>();
		refs.add(FileLinkRequestedEvent.Ref.of(FileRef.of(race), RaceImageRequestDto.toItems(request.getImages())));
		imagesByCourse.forEach((course, images) ->
			refs.add(FileLinkRequestedEvent.Ref.of(FileRef.of(course), RaceImageRequestDto.toItems(images))));
		// 파일 소유자는 대회를 등록한 회원이다 (파일을 올린 회원과 같아야 연결된다)
		eventPublisher.publishEvent(RaceSavedEvent.of(race, List.of(), new FileLinkRequestedEvent(memberId, refs)));
		return race;
	}
}
