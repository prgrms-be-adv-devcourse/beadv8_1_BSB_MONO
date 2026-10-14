package com.bukang.product.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 검색창 연관검색어. 대회 이름과 종목 이름("대회명 종목명")을 나눠 준다
@AllArgsConstructor
@Getter
public class RaceSuggestionDto {
	private final List<RaceTitleSuggestionDto> races;
	private final List<CourseLabelSuggestionDto> courses;
}
