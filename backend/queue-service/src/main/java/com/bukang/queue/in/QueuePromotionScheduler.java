package com.bukang.queue.in;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bukang.queue.app.QueueFacade;

import lombok.RequiredArgsConstructor;

/**
 * 대기열(대회·종목)마다 끝 시각이 지난 active를 지우고, waiting 앞에서부터 정해진 인원을 active로 승격한다
 * 주기·인원·활성 시간은 application.yaml의 queue.*에 있다
 * 테스트에서는 등록하지 않는다. 테스트는 QueuePromoteUseCase에 시각을 넣어 직접 부른다
 */
@Profile("!test")
@Component
@RequiredArgsConstructor
public class QueuePromotionScheduler {
	private final QueueFacade queueFacade;

	// 앞 실행이 끝난 뒤 queue.schedule.time(ms)만큼 쉬고 다시 돈다
	@Scheduled(fixedDelayString = "${queue.schedule.time}", initialDelayString = "${queue.schedule.time}")
	public void promote() {
		queueFacade.promote();
	}
}
