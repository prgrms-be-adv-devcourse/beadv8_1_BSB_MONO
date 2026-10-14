package com.bukang.order.out.seat;

import java.time.LocalDateTime;

/**
 * 상품 서비스(B)에 참가권 자리 1석을 확보한다. 자리 행(RACE_SEAT)과 정원은 B 소유다 (ERD 추천안 v4, 팀 결정 #10)
 * 결제 기한(개인 10분)은 주문이 정해 보내고 B가 그 시각까지 자리를 잡는다. 결제와 만료 판정의 원본은 B에 저장된 expires_at이다
 */
public interface SeatHoldPort {
	// 결제 기한(expiresAt)은 주문이 정해 보낸다 (10/14 김호빈님과 정함, 대기열이 생기면 활성 시간이 끝나는 시각)
	// 멱등: 같은 참가 신청 ID로 다시 부르면 새 자리를 잡지 않고 처음 결과(처음 기한)를 돌려준다
	SeatHoldResult hold(int raceCourseId, int participationId, LocalDateTime expiresAt);

	// 결제 뒤 자리를 판매 완료로 바꾼다. 기한이 지났으면 B가 거절한다 (결제·만료 판정 원본은 B의 expires_at)
	// 멱등: 이미 판매한 자리면 기한과 상관없이 SOLD다. 판매 뒤 주문 저장이 롤백되어 같은 주문으로 다시 결제할 때,
	// 기한이 지났다고 EXPIRED를 받으면 예치금은 돌려주는데 자리는 팔린 채로 남는다
	SeatSellResult sell(int participationId);

	// 결제한 참가권을 취소하면 자리를 판매 가능 정원으로 되돌린다 (SOLD → RESTORED). 여러 번 불러도 한 번만 되돌린다
	void restore(int participationId);

	// 결제하지 않고 기한이 지난 자리를 풀어 달라고 알린다 (HELD → RELEASED). B는 자리를 스스로 풀지 않는다 (10/14 정함)
	// 답을 기다리지 않는 알림(Kafka product.hold.cancel)이고, 이미 판매·해제된 자리면 B가 아무것도 하지 않는다 (멱등)
	void release(int participationId);
}
