package com.bukang.queue.app;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.bukang.queue.config.QueueProperties;
import com.bukang.queue.domain.exception.QueueUnavailableException;
import com.bukang.queue.out.QueueRedisRepository;
import com.bukang.queue.out.QueueRedisRepository.Promotion;
import com.bukang.queue.out.QueueRedisRepository.QueueId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueuePromoteUseCase {
	private final QueueRedisRepository queueRedisRepository;
	private final QueueProperties queueProperties;

	// 스케줄러 목록(queue:waiting)의 대기열마다 끝 시각이 지난 active를 지우고, waiting 앞에서부터 정해진 인원을 승격한다
	// 한 대기열에서 오류가 나도 그 대기열만 건너뛰고 다음 대기열을 승격한다. 건너뛴 대기열은 다음 주기에 다시 한다
	// 돌려주는 값은 이번 주기에 지우고 승격한 인원의 합계다
	public Promotion promote(LocalDateTime now) {
		LocalDateTime activeUntil = now.plus(queueProperties.active().time());
		int size = queueProperties.schedule().size();

		Set<String> queueIds;
		try {
			queueIds = queueRedisRepository.findQueueIds();
		} catch (QueueUnavailableException exception) {
			// 저장소가 원인과 함께 로그를 남겼다. 이번 주기는 건너뛰고 다음 주기에 다시 한다
			return new Promotion(0, 0);
		}

		long expired = 0;
		long promoted = 0;
		for (String queueId : queueIds) {
			try {
				QueueId queue = QueueId.parse(queueId);
				Promotion promotion = queueRedisRepository.promote(queue, now, activeUntil, size);
				// 확인용 로그. dev(com.bukang: debug)에서만 보이고 prod(info)에는 남지 않는다
				if (promotion.expired() > 0 || promotion.promoted() > 0) {
					log.debug("대기열을 승격했습니다. raceId={}, raceCourseId={}, 승격={}명, 만료={}명",
						queue.raceId(), queue.raceCourseId(), promotion.promoted(), promotion.expired());
				}
				expired += promotion.expired();
				promoted += promotion.promoted();
			} catch (QueueUnavailableException exception) {
				// 저장소가 원인과 함께 로그를 남겼다. 이 대기열만 건너뛴다
			} catch (RuntimeException exception) {
				log.error("대기열을 승격하지 못했습니다. queueId={}", queueId, exception);
			}
		}
		return new Promotion(expired, promoted);
	}
}
