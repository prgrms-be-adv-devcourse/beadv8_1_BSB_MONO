package com.bukang.product.dto;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import com.bukang.product.domain.Race;
import com.bukang.product.domain.RaceCourse;
import com.bukang.product.domain.RaceSaleStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 판매자 대회 관리의 대회 한 줄 (취소된 대회도 나온다)
@AllArgsConstructor
@Getter
public class SellerRaceDto {
	private final int raceId;
	private final String title;
	private final LocalDateTime raceDate;
	private final RaceSaleStatus saleStatus;
	private final LocalDateTime saleOpenAt;
	private final LocalDateTime applicationCloseAt;
	private final List<SellerRaceCourseDto> courses;

	public static SellerRaceDto from(Race race) {
		List<SellerRaceCourseDto> courses = race.getCourses().stream()
			.sorted(Comparator.comparingInt(RaceCourse::getId))
			.map(SellerRaceCourseDto::from)
			.toList();
		return new SellerRaceDto(race.getId(), race.getTitle(), race.getRaceDate(), race.getSaleStatus(),
			race.getSaleOpenAt(), race.getApplicationCloseAt(), courses);
	}
}
