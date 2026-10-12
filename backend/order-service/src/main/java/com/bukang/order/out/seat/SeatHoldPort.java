package com.bukang.order.out.seat;

/**
 * 상품 서비스(B)에 참가권 자리 1석을 확보한다. 자리 행(RACE_SEAT)과 정원은 B 소유다 (ERD 추천안 v4, 팀 결정 #10)
 * 결제 기한(개인 10분)은 B가 정해 돌려준다. 결제와 만료 판정의 원본도 B의 expires_at이다
 */
public interface SeatHoldPort {
	SeatHoldResult hold(int raceCourseId, int participationId, int memberId);

	// PG 충전창에 들어가면 결제 기한을 2분 늘린다. 자리마다 한 번만 된다 (정책 4장 결제)
	SeatExtendResult extend(int participationId);

	// 결제 뒤 자리를 판매 완료로 바꾼다. 기한이 지났으면 B가 거절한다 (결제·만료 판정 원본은 B의 expires_at)
	// 멱등: 이미 판매한 자리면 기한과 상관없이 SOLD다. 판매 뒤 주문 저장이 롤백되어 같은 주문으로 다시 결제할 때,
	// 기한이 지났다고 EXPIRED를 받으면 예치금은 돌려주는데 자리는 팔린 채로 남는다
	SeatSellResult sell(int participationId);

	// 결제한 참가권을 취소하면 자리를 판매 가능 정원으로 되돌린다 (SOLD → RESTORED). 여러 번 불러도 한 번만 되돌린다
	void restore(int participationId);
}
