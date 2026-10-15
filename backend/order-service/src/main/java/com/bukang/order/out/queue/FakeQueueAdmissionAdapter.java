package com.bukang.order.out.queue;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 가짜 어댑터: 대기열 없이 개발할 때, 누구든 지금 막 입장한 것으로 보고 10분 뒤를 입장 끝 시각으로 돌려준다
 * (정책 4장 2-4 "입장 뒤 10분 안에 신청서 + 결제"). 대기열 없이 [결제하기]를 하던 지금까지와 결제 기한이 같다
 * order.clients.queue.mode=fake(기본값)일 때만 쓴다. 대기열 내부 API(PRO-182)가 나오면 HTTP 어댑터를 붙인다
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "order.clients.queue", name = "mode", havingValue = "fake", matchIfMissing = true)
public class FakeQueueAdmissionAdapter implements QueueAdmissionPort {
	static final long ADMISSION_MINUTES = 10;

	// 호출마다 경고를 남기면 로그가 묻히므로, 앱을 켤 때 한 번만 알린다
	public FakeQueueAdmissionAdapter() {
		log.warn("대기열 입장 확인은 가짜 응답(FakeQueueAdmissionAdapter)을 씁니다. 누구든 입장 중으로 봅니다");
	}

	@Override
	public Optional<LocalDateTime> findAdmissionEndsAt(int memberId, int raceId, int raceCourseId,
		LocalDateTime now) {
		return Optional.of(now.plusMinutes(ADMISSION_MINUTES));
	}
}
