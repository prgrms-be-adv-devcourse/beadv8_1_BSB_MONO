package com.bukang.queue.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 대기열 승격 설정 (queue.*).
 *
 * @param schedule 승격 주기와 한 번에 승격할 인원
 * @param active 승격한 회원이 입장(active)해 있는 시간
 */
@ConfigurationProperties("queue")
public record QueueProperties(Schedule schedule, Active active) {

	public QueueProperties {
		if (schedule == null || active == null) {
			throw new IllegalArgumentException("queue.schedule과 queue.active를 설정해야 합니다.");
		}
	}

	/**
	 * @param time 승격 주기 (숫자만 쓰면 ms). 스케줄러는 @Scheduled에서 이 값을 직접 읽는다
	 * @param size 대기열(대회·종목)마다 한 번에 승격할 인원
	 */
	public record Schedule(Duration time, int size) {
		public Schedule {
			if (time == null || time.isNegative() || time.isZero()) {
				throw new IllegalArgumentException("queue.schedule.time은 0보다 커야 합니다: " + time);
			}
			if (size <= 0) {
				throw new IllegalArgumentException("queue.schedule.size는 0보다 커야 합니다: " + size);
			}
		}
	}

	/**
	 * @param time 활성 시간 (숫자만 쓰면 ms). 승격 시각 + 이 시간이 active 끝 시각이다
	 */
	public record Active(Duration time) {
		public Active {
			if (time == null || time.isNegative() || time.isZero()) {
				throw new IllegalArgumentException("queue.active.time은 0보다 커야 합니다: " + time);
			}
		}
	}
}
