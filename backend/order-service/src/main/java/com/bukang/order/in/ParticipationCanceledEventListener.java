package com.bukang.order.in;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bukang.order.domain.event.ParticipationCanceledBeforePaymentEvent;
import com.bukang.order.out.seat.SeatHoldPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 결제 전 신청서를 취소하면(결제 전 취소 API, 같은 대회 재신청으로 교체) 상품 서비스에 그 신청서의 자리를 풀어 달라고 알린다
 * 자리가 없거나 이미 판매·해제된 자리면 상품이 아무것도 하지 않으므로(멱등) 자리를 잡았는지 몰라도 보낸다
 * 만료(OrderExpiredEventListener)와 달리 커밋 뒤에 보낸다. 상품은 한 번 풀린 신청서로 다시 자리를 잡아 주지 않으므로,
 * 보낸 뒤 취소가 롤백되면 살아 있는 신청서가 결제할 수 없게 된다. 보내지 못하면 자리가 묶인 채 남아 로그로 남긴다(대조 대상)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParticipationCanceledEventListener {
	private final SeatHoldPort seatHoldPort;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void releaseSeat(ParticipationCanceledBeforePaymentEvent event) {
		try {
			seatHoldPort.release(event.participationId());
		} catch (RuntimeException exception) {
			// 취소는 이미 커밋됐다(커밋 뒤 예외는 스프링도 응답에 넘기지 않는다). 묶인 자리를 대조할 수 있게 신청서 ID를 남긴다
			log.warn("결제 전 취소한 신청서의 자리 해제를 보내지 못했습니다. 상품 자리를 확인해야 합니다. participationId={}, cause={}",
				event.participationId(), exception.toString());
		}
	}
}
