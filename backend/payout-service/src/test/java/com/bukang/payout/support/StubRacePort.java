package com.bukang.payout.support;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;

import com.bukang.payout.out.RacePort;
import com.bukang.payout.out.RaceSchedule;

/**
 * 테스트용 상품 서비스: 대회 이름과 대회 일정(환불 마감일)을 케이스마다 정해 돌려준다
 * 실제 앱의 FakeRaceAdapter는 환불 마감일을 모르므로 1차 대상을 돌려주지 않는다
 */
public class StubRacePort implements RacePort {
	private final IntFunction<Optional<String>> raceNames;
	private final List<RaceSchedule> schedules;

	public StubRacePort(IntFunction<Optional<String>> raceNames, List<RaceSchedule> schedules) {
		this.raceNames = raceNames;
		this.schedules = schedules;
	}

	@Override
	public Optional<String> findRaceName(int raceId) {
		return raceNames.apply(raceId);
	}

	@Override
	public List<RaceSchedule> findRacesByRefundDeadline(LocalDate fromRefundDeadline, LocalDate toRefundDeadline) {
		return schedules.stream()
			.filter(race -> !race.refundDeadline().isBefore(fromRefundDeadline)
				&& !race.refundDeadline().isAfter(toRefundDeadline))
			.toList();
	}

	@Override
	public Optional<LocalDate> findRefundDeadline(int raceId) {
		return schedules.stream()
			.filter(race -> race.raceId() == raceId)
			.map(RaceSchedule::refundDeadline)
			.findFirst();
	}
}
