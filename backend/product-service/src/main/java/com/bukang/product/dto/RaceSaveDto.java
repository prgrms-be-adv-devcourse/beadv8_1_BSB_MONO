package com.bukang.product.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.bukang.product.domain.RaceSaleStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class RaceSaveDto {
	private final int raceId;
	private final RaceSaleStatus saleStatus;
	// 크루 예매 시작·개인 예매 오픈 중 이른 시각 (서버가 채운다)
	private final LocalDateTime saleOpenAt;
	// 요청한 종목 순서대로. 수정할 때 각 종목의 courseId로 보낸다
	private final List<Integer> courseIds;
}
