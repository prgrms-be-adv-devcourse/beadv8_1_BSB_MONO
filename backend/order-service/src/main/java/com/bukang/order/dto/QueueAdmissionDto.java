package com.bukang.order.dto;

import java.time.Duration;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 대기열 입장 상태 (결제 페이지 타이머용, PRO-191). 입장 끝 시각이 [결제하기]의 결제 기한이 된다
 * remainingSeconds는 서버 시각으로 센 남은 초라, 프론트 기기 시계가 틀려도 타이머가 어긋나지 않는다
 */
@AllArgsConstructor
@Getter
public class QueueAdmissionDto {
	// 입장 중인지. 아니면 [결제하기]가 403이다
	private final boolean admitted;
	// 입장이 끝나는 시각. 입장 중이 아니면 null
	private final LocalDateTime admissionEndsAt;
	// 남은 초. 입장 중이 아니면 0
	private final long remainingSeconds;

	public static QueueAdmissionDto of(LocalDateTime admissionEndsAt, LocalDateTime now) {
		if (admissionEndsAt == null || !admissionEndsAt.isAfter(now)) {
			return new QueueAdmissionDto(false, null, 0);
		}
		// 1초 미만이 남아도 입장 중이므로 올림으로 센다 (입장 중인데 0초로 보이지 않게)
		long remainingMillis = Duration.between(now, admissionEndsAt).toMillis();
		return new QueueAdmissionDto(true, admissionEndsAt, (remainingMillis + 999) / 1000);
	}
}
