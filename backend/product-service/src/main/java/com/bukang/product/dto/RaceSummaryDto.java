package com.bukang.product.dto;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import com.bukang.product.domain.Race;
import com.bukang.product.domain.RaceCourse;
import com.bukang.product.domain.RaceSaleStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 공개 대회 목록의 대회 한 줄. 목록 카드는 대회 정보로 만든 색 포스터를 쓰므로 이미지는 넣지 않는다 (정책 2장 이미지 규격)
@AllArgsConstructor
@Getter
public class RaceSummaryDto {
	private final int raceId;
	private final String title;
	private final String location;
	private final String regionSido;
	private final String regionSigungu;
	private final LocalDateTime raceDate;
	private final RaceSaleStatus saleStatus;
	private final LocalDateTime saleOpenAt;
	private final LocalDateTime applicationCloseAt;
	// 조건에 맞은 종목 중 가장 싼 참가비 ("₩45,000부터"). 조건이 없으면 대회에서 가장 싼 종목의 참가비
	//   "하프"만 골랐으면 5K가 더 싸도 하프 종목의 참가비
	private final int minPrice;
	private final List<RaceSummaryCourseDto> courses;

	public static RaceSummaryDto from(Race race, int minPrice) {
		List<RaceSummaryCourseDto> courses = race.getCourses().stream()
			.sorted(Comparator.comparingInt(RaceCourse::getId))
			.map(RaceSummaryCourseDto::from)
			.toList();
		return new RaceSummaryDto(race.getId(), race.getTitle(), race.getLocation(), race.getRegionSido(),
			race.getRegionSigungu(), race.getRaceDate(), race.getSaleStatus(), race.getSaleOpenAt(),
			race.getApplicationCloseAt(), minPrice, courses);
	}
}
