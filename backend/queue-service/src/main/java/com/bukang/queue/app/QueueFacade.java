package com.bukang.queue.app;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.common.shared.race.domain.RaceSaleStatus;
import com.bukang.queue.domain.QueueRace;
import com.bukang.queue.domain.QueueRaceCourse;
import com.bukang.queue.dto.QueueStatusDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QueueFacade {
	private final QueueSyncRaceUseCase queueSyncRaceUseCase;
	private final QueueSyncRaceCourseUseCase queueSyncRaceCourseUseCase;
	private final QueueRegisterUseCase queueRegisterUseCase;

	// 트랜잭션을 걸지 않는다. 복제본 조회(대회·종목)는 각각 짧게 끝나고,
	// Redis를 기다리는 동안(장애 때는 최대 2초) DB 커넥션을 잡고 있지 않게 한다
	public QueueStatusDto register(int memberId, int raceId, int raceCourseId) {
		return queueRegisterUseCase.register(memberId, raceId, raceCourseId, LocalDateTime.now());
	}

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
