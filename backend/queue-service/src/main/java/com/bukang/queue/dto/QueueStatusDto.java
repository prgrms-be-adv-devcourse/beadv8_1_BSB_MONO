package com.bukang.queue.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 대기열 상태. 등록과 순번 조회가 같은 모양으로 돌려준다
@AllArgsConstructor
@Getter
public class QueueStatusDto {
	// 내 순번 (1부터)
	private final long myRank;
	// 내 앞에서 기다리는 인원
	private final long ahead;
	// 줄 전체 인원
	private final long total;
	// 입장할 수 있는지 (active로 승격됐는지). 등록 응답에서는 늘 false다 (active 회원은 등록을 거부한다)
	private final boolean canEnter;

	// 줄에서 기다리는 중. rank는 내 앞 인원(0부터)이다
	public static QueueStatusDto waiting(long rank, long total) {
		return new QueueStatusDto(rank + 1, rank, total, false);
	}
}
