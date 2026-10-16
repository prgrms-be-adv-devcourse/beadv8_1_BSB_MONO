package com.bukang.payout.out;

/**
 * 1차 정산용 판매 집계: 한 대회(= 코스)에서 판매자의 결제 완료 · 미취소 참가권 가격 합과 장수 (PRO-214)
 * 대회 하나는 코스 하나라 참가권 가격이 하나이고 수량은 항상 1이다. 그래서 가격 합 ÷ 장수가 참가권 1장 가격이다
 */
public record SoldSummary(int sellerId, int raceId, long priceSum, int ticketCount) {
}
