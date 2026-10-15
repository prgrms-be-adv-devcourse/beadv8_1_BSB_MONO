package com.bukang.queue.app;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.common.shared.race.domain.RaceSaleStatus;
import com.bukang.queue.domain.QueueRace;
import com.bukang.queue.domain.QueueRaceCourse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QueueFacade {
	private final QueueSyncRaceUseCase queueSyncRaceUseCase;
	private final QueueSyncRaceCourseUseCase queueSyncRaceCourseUseCase;

	@Transactional
	public QueueRace syncRace(
		int raceId,
		RaceSaleStatus saleStatus,
		LocalDateTime individualOpenAt,
		LocalDateTime applicationCloseAt,
		LocalDateTime updatedAt
	) {
		return queueSyncRaceUseCase.syncRace(raceId, saleStatus, individualOpenAt, applicationCloseAt, updatedAt);
	}

	@Transactional
	public QueueRaceCourse syncRaceCourse(int raceCourseId, int raceId, LocalDateTime updatedAt, boolean deleted) {
		return queueSyncRaceCourseUseCase.syncRaceCourse(raceCourseId, raceId, updatedAt, deleted);
	}
}
