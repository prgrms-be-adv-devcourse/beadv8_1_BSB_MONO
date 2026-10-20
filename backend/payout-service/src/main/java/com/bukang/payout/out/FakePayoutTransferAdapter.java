package com.bukang.payout.out;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 테스트용 어댑터: 캐시 서비스를 띄우지 않는 테스트 프로파일에서 정산금 이동을 늘 성공으로 본다 (PRO-252)
 * 지급 배치 테스트는 이 빈 대신 결과를 정한 포트를 직접 넣으므로, 이 어댑터는 스프링 컨텍스트를 띄우기 위해서만 있다
 * dev(로컬)와 prod는 CashPayoutTransferAdapter가 실제 캐시 서비스를 부른다
 */
@Component
@Profile("test")
public class FakePayoutTransferAdapter implements PayoutTransferPort {
	@Override
	public void transfer(PayoutTransfer transfer) {
		// 옮길 돈이 없으므로 아무것도 하지 않는다
	}
}
