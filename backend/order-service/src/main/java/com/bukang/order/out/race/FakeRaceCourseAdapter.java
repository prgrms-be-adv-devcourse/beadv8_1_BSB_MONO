package com.bukang.order.out.race;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 임시 어댑터: 상품 서비스(B)가 아직 없어, 어떤 종목 ID든 지금 접수 중인 종목으로 돌려준다
 * - 대회 ID는 종목 ID와 같게 둔다 (종목 하나 = 대회 하나)
 * - 대회는 60일 뒤, 접수는 하루 전에 열려 30일 뒤에 마감, 만 14세 이상
 * - 판매자 ID 1, 참가비 50,000원 + 키트 10,000원
 * 상품 서비스와 연결할 때(HTTP 호출) 이 클래스를 진짜 어댑터로 바꾼다
 */
@Slf4j
@Component
public class FakeRaceCourseAdapter implements RaceCoursePort {
	// 호출마다 경고를 남기면 로그가 묻히므로, 앱을 켤 때 한 번만 알린다
	public FakeRaceCourseAdapter() {
		log.warn("상품 서비스 연결 전이라 종목 정보는 가짜 응답(FakeRaceCourseAdapter)을 씁니다.");
	}

	@Override
	public Optional<RaceCourseInfo> findCourse(int raceCourseId) {
		if (raceCourseId <= 0) {
			return Optional.empty();
		}
		log.debug("가짜 종목 정보를 돌려줍니다. raceCourseId={}", raceCourseId);
		LocalDateTime now = LocalDateTime.now();
		return Optional.of(new RaceCourseInfo(
			raceCourseId,
			raceCourseId,
			now.plusDays(60),
			now.minusDays(1),
			now.plusDays(30),
			14,
			null,
			1,
			"가짜 대회 %d 종목".formatted(raceCourseId),
			50_000,
			10_000
		));
	}
}
