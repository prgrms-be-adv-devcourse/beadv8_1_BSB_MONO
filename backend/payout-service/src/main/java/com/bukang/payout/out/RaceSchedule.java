package com.bukang.payout.out;

import java.time.LocalDate;

/**
 * 1차 정산 대상을 고를 때 상품 서비스에서 받는 대회 일정 (PRO-32)
 * 1차 정산은 환불 마감일 다음 날 만들고, 정산 월은 환불 마감일의 달이다
 * 환불 마감 뒤에는 환불 마감일이 움직이지 않는다고 가정한다 (일정 변경 · 대회 취소 환불은 PRO-174, 실제 연결은 PRO-187)
 */
public record RaceSchedule(int raceId, LocalDate raceDate, LocalDate refundDeadline) {
}
