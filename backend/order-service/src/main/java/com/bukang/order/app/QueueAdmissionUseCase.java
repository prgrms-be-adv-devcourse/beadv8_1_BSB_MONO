package com.bukang.order.app;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.bukang.order.dto.QueueAdmissionDto;
import com.bukang.order.out.queue.QueueAdmissionPort;

import lombok.RequiredArgsConstructor;

/**
 * 결제 페이지 타이머에 쓸 대기열 입장 상태를 돌려준다 (PRO-191)
 * [결제하기](OrderCreateUseCase)와 같은 포트를 물어, 타이머와 결제 기한이 같은 입장 끝 시각을 쓴다
 * 대기열 내부 API(PRO-182)가 나오면 어댑터만 바뀌고 프론트가 부르는 주소는 그대로다
 */
@Service
@RequiredArgsConstructor
public class QueueAdmissionUseCase {
	private final QueueAdmissionPort queueAdmissionPort;

	public QueueAdmissionDto find(int memberId, int raceId, int raceCourseId, LocalDateTime now) {
		LocalDateTime endsAt = queueAdmissionPort.findAdmissionEndsAt(memberId, raceId, raceCourseId, now)
			.orElse(null);
		return QueueAdmissionDto.of(endsAt, now);
	}
}
