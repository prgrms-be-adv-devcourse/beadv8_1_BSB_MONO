package com.bukang.order.support;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.bukang.order.out.race.RaceCourseInfo;
import com.bukang.order.out.race.RaceCoursePort;

/**
 * 테스트용 종목 정보. 테스트마다 접수 기간·나이 제한을 원하는 대로 넣는다
 */
public class StubRaceCoursePort implements RaceCoursePort {
	private final Map<Integer, RaceCourseInfo> courses = new HashMap<>();

	@Override
	public Optional<RaceCourseInfo> findCourse(int raceCourseId) {
		return Optional.ofNullable(courses.get(raceCourseId));
	}

	public void put(RaceCourseInfo course) {
		courses.put(course.raceCourseId(), course);
	}

	public void clear() {
		courses.clear();
	}

	// 지금 접수 중이고 대회는 60일 뒤인 종목
	public static RaceCourseInfo openCourse(int raceCourseId, int raceId, int minAge, Integer maxAge) {
		LocalDateTime now = LocalDateTime.now();
		return new RaceCourseInfo(raceCourseId, raceId, now.plusDays(60), now.minusDays(1), now.plusDays(30),
			minAge, maxAge);
	}

	// 접수가 이미 마감된 종목
	public static RaceCourseInfo closedCourse(int raceCourseId, int raceId) {
		LocalDateTime now = LocalDateTime.now();
		return new RaceCourseInfo(raceCourseId, raceId, now.plusDays(10), now.minusDays(30), now.minusDays(1),
			14, null);
	}
}
