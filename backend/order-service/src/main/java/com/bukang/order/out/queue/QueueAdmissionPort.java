package com.bukang.order.out.queue;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 대기열(PRO-182)에 회원이 이 종목에 입장 중인지 묻는다 (PRO-191)
 * 개인 예매는 모두 대기열을 거쳐 입장한 뒤 [결제하기]를 한다. 입장은 정해진 시간(TTL)이 지나면 대기열이 스스로 끝낸다
 * 읽기만 하므로 몇 번 불러도 된다. 결제 뒤 입장을 반납하지 않는다 (10/15 호빈님과 정함)
 */
public interface QueueAdmissionPort {
	/**
	 * @return 입장 중이면 입장이 끝나는 시각, 입장 중이 아니면(줄 서는 중, 입장 끝남, 대기열에 없음) 빈 값
	 * 대기열에 연결하지 못하거나 응답이 이상하면 빈 값이 아니라 ExternalServiceUnavailableException(503)을 던진다
	 * (빈 값을 돌려주면 장애를 "입장 전" 403으로 잘못 알리게 된다)
	 */
	Optional<LocalDateTime> findAdmissionEndsAt(int memberId, int raceId, int raceCourseId, LocalDateTime now);
}
