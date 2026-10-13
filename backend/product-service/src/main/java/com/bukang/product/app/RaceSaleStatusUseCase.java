package com.bukang.product.app;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.bukang.product.domain.RaceSaleStatus;
import com.bukang.product.domain.document.RaceCourseDocument;
import com.bukang.product.out.RaceRepository;

import lombok.RequiredArgsConstructor;

/**
 * 시각이 지난 대회의 판매 상태를 바꾼다 (RaceSaleStatusScheduler가 5분마다 부른다)
 * - 판매 예정·판매 중 → 판매 종료: 대회일이 지난 대회 (대회 날짜 다음 날 0시부터)
 * - 판매 예정 → 판매 중: 판매 시작 일시가 지난 대회
 * 판매 종료를 먼저 바꿔, 판매 시작과 대회일이 모두 지난 대회가 판매 중을 거치지 않게 한다.
 * 작성 중·판매 중지는 판매자가 바꾸는 상태라 건드리지 않는다.
 * 상태는 조건부 UPDATE로 바꾼다. 판매자가 같은 대회를 수정해도 엔티티는 바뀐 컬럼만 저장하므로(@DynamicUpdate) 덮어쓰지 않는다.
 * 대상 ID를 읽은 뒤 판매자가 대회일을 미루면 UPDATE 조건에서 걸러져 판매 종료되지 않는다.
 */
@Service
@RequiredArgsConstructor
public class RaceSaleStatusUseCase {
	private static final List<RaceSaleStatus> OPEN_STATUSES = List.of(RaceSaleStatus.SCHEDULED, RaceSaleStatus.ON_SALE);

	private final RaceRepository raceRepository;

	// 상태가 바뀐 대회의 검색 문서를 돌려준다 (커밋한 뒤 ProductFacade가 다시 색인한다)
	public List<RaceCourseDocument> refresh(LocalDateTime now) {
		List<Integer> changedIds = new ArrayList<>();

		LocalDateTime today = now.toLocalDate().atStartOfDay();
		List<Integer> toClose = raceRepository.findIdsToClose(OPEN_STATUSES, today);
		if (!toClose.isEmpty()) {
			raceRepository.closeSale(toClose, OPEN_STATUSES, today, now);
			changedIds.addAll(toClose);
		}
		List<Integer> toOpen = raceRepository.findIdsToOpen(RaceSaleStatus.SCHEDULED, now);
		if (!toOpen.isEmpty()) {
			raceRepository.openSale(toOpen, now);
			changedIds.addAll(toOpen);
		}

		// 바꾼 상태를 DB에서 다시 읽어 검색 문서를 만든다 (조건부 UPDATE에서 건너뛴 대회는 지금 상태 그대로 색인된다)
		return raceRepository.findAllById(changedIds).stream()
			.flatMap(race -> race.getCourses().stream())
			.map(RaceCourseDocument::from)
			.toList();
	}
}
