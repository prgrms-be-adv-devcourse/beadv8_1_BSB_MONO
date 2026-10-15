package com.bukang.payout.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 임시 어댑터: 상품 서비스(B)가 아직 없어, 어떤 대회 ID든 "대회 {ID}"라는 이름을 돌려준다
 * 환불 마감일은 모르므로 1차 정산 대상 대회를 하나도 돌려주지 않는다. 가짜 날짜로 1차 정산(실제 돈)을 만들지 않기 위해서다
 * 그래서 이 어댑터를 쓰는 동안 1차 정산은 만들어지지 않고, 최종 정산은 선지급 차감 없이 만들어진다 (PRO-190)
 * 상품 서비스와 연결할 때(HTTP 호출) 이 클래스를 진짜 어댑터로 바꾼다 (주문 서비스의 FakeRaceCourseAdapter와 같은 방식)
 */
@Slf4j
@Component
public class FakeRaceAdapter implements RacePort {
	// 호출마다 경고를 남기면 로그가 묻히므로, 앱을 켤 때 한 번만 알린다
	public FakeRaceAdapter() {
		log.warn("상품 서비스 연결 전이라 대회 이름은 가짜 응답(FakeRaceAdapter)을 씁니다.");
	}

	@Override
	public Optional<String> findRaceName(int raceId) {
		if (raceId <= 0) {
			return Optional.empty();
		}
		return Optional.of("대회 %d".formatted(raceId));
	}

	@Override
	public List<RaceSchedule> findRacesByRefundDeadline(LocalDate fromRefundDeadline, LocalDate toRefundDeadline) {
		return List.of();
	}

	// 환불 마감일을 모르므로, 환불 가능 여부는 대회일 기준(대회 다음 날부터 불가)으로 판단된다
	@Override
	public Optional<LocalDate> findRefundDeadline(int raceId) {
		return Optional.empty();
	}
}
