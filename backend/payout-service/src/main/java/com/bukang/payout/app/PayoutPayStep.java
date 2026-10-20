package com.bukang.payout.app;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.payout.domain.Payout;
import com.bukang.payout.domain.PayoutStatus;
import com.bukang.payout.domain.exception.PayoutNotFoundException;
import com.bukang.payout.out.PayoutRepository;
import com.bukang.payout.out.PayoutTransfer;

import lombok.RequiredArgsConstructor;

/**
 * 지급 배치의 정산 내역 한 건 단계마다 짧은 트랜잭션을 연다 (PRO-252)
 * 캐시 서비스 호출(HTTP)은 이 트랜잭션들 밖에서 한다. 호출하는 동안 정산 DB 연결과 행 잠금을 잡지 않기 위해서다
 * 지급 중(PAYING)을 호출 전에 커밋해 두므로, 트랜잭션 밖에 있어도 그동안 보류 · 상계 · 취소가 끼어들지 못한다
 * PayoutPayUseCase가 부른다. 같은 클래스 안에서 부르면 @Transactional 프록시를 거치지 않으므로 빈을 나눈다
 */
// TODO: app 패키지에는 UseCase · Facade만 둔다는 규칙과 어긋난다. 이름을 UseCase로 바꿀지,
//  PayoutPayUseCase에 TransactionTemplate으로 합칠지 고민해 보기 (PayoutGroupCreator도 같은 문제)
@Component
@RequiredArgsConstructor
public class PayoutPayStep {
	private final PayoutRepository payoutRepository;

	@Transactional(readOnly = true)
	public List<Integer> findTargetIds(LocalDate today) {
		return payoutRepository.findPayTargetIds(today, PayoutStatus.CALCULATED, PayoutStatus.PAYING);
	}

	/**
	 * 지급을 시작한다. 대상을 고른 뒤 상태가 바뀌었을 수 있으므로 다시 읽어 판단한다
	 * - 지급 예정일이 된 정산 예정: 옮길 돈이 있으면 지급 중으로 바꿔 커밋하고, 없으면(대금 · 수수료 0원) 바로 지급 완료한다
	 * - 지급 중: 지난 실행에서 결과를 모른 건이다. 상태를 그대로 두고 같은 금액으로 다시 요청한다
	 *   (지급 중에는 상계가 지급액을 바꾸지 못하므로 처음 요청과 금액이 같다)
	 * - 그 밖(보류 · 지급 완료 · 취소, 아직 지급 예정일 전): 건너뛴다
	 * 다른 배치가 같은 정산 내역을 먼저 지급 중으로 커밋했으면, 커밋할 때 낙관적 락 예외가 난다. 부르는 쪽이 건너뛴다
	 */
	@Transactional
	public Start start(int payoutId, LocalDate today, LocalDateTime now) {
		Payout payout = payoutRepository.findById(payoutId).orElseThrow(PayoutNotFoundException::new);
		if (payout.getStatus() == PayoutStatus.PAYING) {
			return Start.transfer(PayoutTransfer.from(payout));
		}
		if (payout.getStatus() != PayoutStatus.CALCULATED || payout.getScheduledPayDate().isAfter(today)) {
			return Start.skipped();
		}
		if (!payout.hasTransferAmount()) {
			payout.pay(now);
			return Start.paidWithoutTransfer();
		}
		payout.startPaying(now);
		return Start.transfer(PayoutTransfer.from(payout));
	}

	// 캐시 서비스가 돈을 옮긴 뒤 지급 완료로 바꾼다
	// 다른 배치가 같은 지급 중 건을 다시 요청해 먼저 지급 완료했으면 바꾸지 않는다 (캐시 서비스는 돈을 한 번만 옮겼다)
	@Transactional
	public boolean complete(int payoutId, LocalDateTime now) {
		Payout payout = payoutRepository.findById(payoutId).orElseThrow(PayoutNotFoundException::new);
		if (payout.getStatus() == PayoutStatus.PAID) {
			return false;
		}
		payout.pay(now);
		return true;
	}

	// 캐시 서비스가 거절해 돈이 옮겨지지 않았으므로 정산 예정으로 되돌린다. 다음 실행에서 다시 지급한다
	@Transactional
	public void revert(int payoutId, LocalDateTime now, String reason) {
		Payout payout = payoutRepository.findById(payoutId).orElseThrow(PayoutNotFoundException::new);
		payout.revertPaying(now, reason);
	}

	/**
	 * 지급 시작 결과
	 *
	 * @param outcome  다음에 할 일
	 * @param transfer TRANSFER일 때 캐시 서비스에 보낼 요청. 그 밖에는 null
	 */
	public record Start(Outcome outcome, PayoutTransfer transfer) {
		static Start transfer(PayoutTransfer transfer) {
			return new Start(Outcome.TRANSFER, transfer);
		}

		static Start paidWithoutTransfer() {
			return new Start(Outcome.PAID_WITHOUT_TRANSFER, null);
		}

		static Start skipped() {
			return new Start(Outcome.SKIPPED, null);
		}
	}

	public enum Outcome {
		// 캐시 서비스에 정산금 이동을 요청한다
		TRANSFER,
		// 옮길 돈이 0원이라 요청 없이 지급 완료했다
		PAID_WITHOUT_TRANSFER,
		// 대상을 고른 뒤 상태가 바뀌어 지급하지 않는다
		SKIPPED
	}
}
