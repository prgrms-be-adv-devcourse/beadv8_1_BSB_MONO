package com.bukang.order.out.payout;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 가짜 어댑터: 정산 서비스를 띄우지 않고 개발할 때, 정산 서비스와 같은 규칙(오늘이 대회 다음 날 전이면 환불 가능)으로 직접 답한다
 * order.clients.payout.mode=fake(기본값)일 때만 쓴다. 실제 정산 서비스는 http로 바꾸면 HttpRefundEligibilityAdapter가 부른다
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "order.clients.payout", name = "mode", havingValue = "fake", matchIfMissing = true)
public class FakeRefundEligibilityAdapter implements RefundEligibilityPort {
	// 정산 서비스와 같은 기준으로 한국 날짜를 쓴다
	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	// 호출마다 경고를 남기면 로그가 묻히므로, 앱을 켤 때 한 번만 알린다
	public FakeRefundEligibilityAdapter() {
		log.warn("환불 가능 여부는 가짜 응답(FakeRefundEligibilityAdapter)을 씁니다. 실제 정산 서비스는 order.clients.payout.mode=http");
	}

	@Override
	public boolean isRefundable(LocalDate raceDate) {
		return LocalDate.now(ZONE).isBefore(raceDate.plusDays(1));
	}
}
