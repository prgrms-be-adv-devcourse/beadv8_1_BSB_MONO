package com.bukang.payout.dto;

import java.time.LocalDate;

/**
 * 지급 배치 한 번의 결과. dev 배치 실행 API 응답에 쓴다
 *
 * @param today 지급 예정일과 비교한 날짜
 * @param paid  지급 완료(PAID)로 바꾼 정산 내역 수
 */
public record PayoutPayResultDto(LocalDate today, int paid) {
}
