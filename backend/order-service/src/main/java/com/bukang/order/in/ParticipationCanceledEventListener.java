package com.bukang.order.in;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.bukang.order.app.OrderOutboxKafkaAppendUseCase;
import com.bukang.order.domain.event.ParticipationCanceledBeforePaymentEvent;

import lombok.RequiredArgsConstructor;

/**
 * 결제 전 신청서를 취소하면(결제 전 취소 API, 같은 대회 재신청으로 교체) 상품 서비스에 그 신청서의 자리를 풀어 달라고 알린다
 * 자리가 없거나 이미 판매·해제된 자리면 상품이 아무것도 하지 않으므로(멱등) 자리를 잡았는지 몰라도 보낸다
 * 취소 트랜잭션 안에서 해제 메시지를 아웃박스에 저장하고, 커밋 뒤 릴레이가 보낸다 (PRO-270)
 * 상품은 한 번 풀린 신청서로 다시 자리를 잡아 주지 않으므로 취소가 롤백되면 보내면 안 되는데, 아웃박스 행도 함께 롤백된다
 * 전에는 커밋 뒤 바로 보내 실패하면 로그만 남고 자리가 묶였다. 이제 커밋된 취소는 릴레이가 다시 보낸다
 */
@Component
@RequiredArgsConstructor
public class ParticipationCanceledEventListener {
	private final OrderOutboxKafkaAppendUseCase orderOutboxKafkaAppendUseCase;

	@EventListener
	public void releaseSeat(ParticipationCanceledBeforePaymentEvent event) {
		orderOutboxKafkaAppendUseCase.appendSeatRelease(event.participationId());
	}
}
