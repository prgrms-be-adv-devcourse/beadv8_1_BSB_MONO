package com.bukang.queue.dto;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 대기열 입장 상태 (주문 서비스의 입장 확인용, 내부 API). 입장 끝 시각이 주문의 자리 확보 기한·결제 기한이 된다
@AllArgsConstructor
@Getter
public class QueueAdmissionDto {
	// 입장(active) 중인지. 줄 서는 중, 입장 시간이 끝남, 대기열에 없음, 대기열(Redis) 장애면 false다
	private final boolean admitted;
	// 입장이 끝나는 시각 (승격 시각 + 활성 시간). 입장 중이 아니면 null
	private final LocalDateTime admissionEndsAt;
	// 끝 시각까지 남은 시간 (ms). 받는 쪽이 자기 시각에 더해 쓰면 서버끼리 시간대가 달라도 맞는다. 입장 중이 아니면 0
	private final long remainingMillis;

	// 입장 중. 저장소가 끝 시각을 ms 단위로 지금과 비교해 걸렀으므로, 같은 ms 기준으로 세면 남은 시간이 늘 1ms 이상이다
	public static QueueAdmissionDto admitted(LocalDateTime admissionEndsAt, LocalDateTime now) {
		long remainingMillis = Duration.between(now.truncatedTo(ChronoUnit.MILLIS), admissionEndsAt).toMillis();
		return new QueueAdmissionDto(true, admissionEndsAt, remainingMillis);
	}

	public static QueueAdmissionDto notAdmitted() {
		return new QueueAdmissionDto(false, null, 0);
	}
}
