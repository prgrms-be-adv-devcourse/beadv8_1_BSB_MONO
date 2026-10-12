package com.bukang.order.out.payout;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 임시 어댑터: 정산 서비스와 아직 연결하지 않아, 정산 서비스와 같은 규칙(오늘이 대회 다음 날 전이면 환불 가능)으로 직접 답한다
 * 정산 서비스와 연결할 때(HTTP 호출, PRO-99) 이 클래스를 진짜 어댑터로 바꾼다
 */
@Slf4j
@Component
public class FakeRefundEligibilityAdapter implements RefundEligibilityPort {
	// 정산 서비스와 같은 기준으로 한국 날짜를 쓴다
	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	// 호출마다 경고를 남기면 로그가 묻히므로, 앱을 켤 때 한 번만 알린다
	public FakeRefundEligibilityAdapter() {
		log.warn("정산 서비스 연결 전이라 환불 가능 여부는 가짜 응답(FakeRefundEligibilityAdapter)을 씁니다.");
	}

	@Override
	public boolean isRefundable(LocalDate raceDate) {
		return LocalDate.now(ZONE).isBefore(raceDate.plusDays(1));
	}
}
