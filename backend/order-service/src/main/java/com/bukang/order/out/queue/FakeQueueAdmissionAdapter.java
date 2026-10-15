package com.bukang.order.out.queue;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 가짜 어댑터: 대기열 내부 API(PRO-182)가 나오기 전까지 대기열 대신 답한다 (10/15 호빈님 요청)
 * - (회원, 종목)마다 입장 끝 시각을 메모리에 기억한다. 처음 물을 때 지금 입장한 것으로 치고, 그 뒤로는 같은 끝 시각을 돌려준다
 *   그래서 프론트 타이머(남은 시간 API)와 [결제하기]의 결제 기한이 같은 시각이 된다
 * - 끝 시각이 지나면 입장 중이 아니다. 다시 입장하려면 enter(dev 더미 API)를 부른다
 * - 입장 시간은 order.clients.queue.fake-admission-minutes(기본 10분, 정책 4장 2-4)로 줄여 만료를 빨리 확인할 수 있다
 * 메모리에만 두므로 앱을 다시 켜면 모두 처음 입장으로 돌아간다
 * order.clients.queue.mode=fake(기본값)일 때만 쓴다. 대기열 내부 API가 나오면 HTTP 어댑터를 붙인다
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "order.clients.queue", name = "mode", havingValue = "fake", matchIfMissing = true)
public class FakeQueueAdmissionAdapter implements QueueAdmissionPort {
	private final long admissionMinutes;
	private final Map<Admission, LocalDateTime> admissionEndsAt = new ConcurrentHashMap<>();

	// 호출마다 경고를 남기면 로그가 묻히므로, 앱을 켤 때 한 번만 알린다
	public FakeQueueAdmissionAdapter(@Value("${order.clients.queue.fake-admission-minutes:10}") long admissionMinutes) {
		this.admissionMinutes = admissionMinutes;
		log.warn("대기열 입장 확인은 가짜 응답(FakeQueueAdmissionAdapter)을 씁니다. 처음 물을 때 입장해 {}분 뒤 끝납니다",
			admissionMinutes);
	}

	@Override
	public Optional<LocalDateTime> findAdmissionEndsAt(int memberId, int raceId, int raceCourseId,
		LocalDateTime now) {
		LocalDateTime endsAt = admissionEndsAt.computeIfAbsent(new Admission(memberId, raceCourseId),
			admission -> now.plusMinutes(admissionMinutes));
		return endsAt.isAfter(now) ? Optional.of(endsAt) : Optional.empty();
	}

	// 대기열을 다시 통과한 것으로 친다 (dev 더미 API). 입장 끝 시각을 지금부터 다시 센다
	public LocalDateTime enter(int memberId, int raceCourseId, LocalDateTime now) {
		LocalDateTime endsAt = now.plusMinutes(admissionMinutes);
		admissionEndsAt.put(new Admission(memberId, raceCourseId), endsAt);
		return endsAt;
	}

	private record Admission(int memberId, int raceCourseId) {
	}
}
