package com.bukang.member.app;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.bukang.member.domain.Member;
import com.bukang.member.domain.exception.MemberNotFoundException;
import com.bukang.member.domain.exception.WithdrawCashLeftException;
import com.bukang.member.domain.exception.WithdrawTicketLeftException;
import com.bukang.member.out.MemberRepository;
import com.bukang.member.out.RefreshTokenRepository;
import com.bukang.member.out.cash.CashBalancePort;
import com.bukang.member.out.order.ActiveTicketPort;

import lombok.RequiredArgsConstructor;

/**
 * 회원 탈퇴 (BE-022). 남은 예치금·참가권이 없을 때만 바로 WITHDRAWN으로 끝내고 개인정보 칸을 비운다
 * 확인 순서: 이미 탈퇴 → 404, 예치금 > 0 → 409, 끝나지 않은 참가권 ≥ 1 → 409, 확인 실패 → 503 (아무것도 바꾸지 않음)
 * 호출하는 쪽(MemberFacade)의 트랜잭션 하나에서 처리하고, 다른 서비스 사본은 커밋 뒤 수정 이벤트로 덮는다
 */
@Service
@RequiredArgsConstructor
public class MemberWithdrawUseCase {
	static final String NOT_FOUND_MESSAGE = "존재하지 않는 회원입니다.";
	static final String CASH_LEFT_MESSAGE = "남은 예치금이 있어 탈퇴할 수 없습니다. 예치금을 모두 사용하거나 출금한 뒤 탈퇴해주세요.";
	static final String TICKET_LEFT_MESSAGE = "끝나지 않은 참가권이 있어 탈퇴할 수 없습니다. 대회가 끝나거나 참가권을 취소한 뒤 탈퇴해주세요.";

	private final MemberRepository memberRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final CashBalancePort cashBalancePort;
	private final ActiveTicketPort activeTicketPort;

	public void withdraw(int memberId) {
		// 줄을 잠가 같은 회원의 탈퇴가 겹치지 않게 한다. 뒤 요청은 앞 요청이 커밋한 뒤 탈퇴한 회원을 보고 404가 된다
		Member member = memberRepository.findByIdForUpdate(memberId)
			.filter(found -> !found.isWithdrawn())
			.orElseThrow(() -> new MemberNotFoundException(NOT_FOUND_MESSAGE));

		// 다른 서비스 확인이 실패하면 예외가 그대로 나가 아무것도 바꾸지 않는다 (503)
		if (cashBalancePort.findBalance(memberId) > 0) {
			throw new WithdrawCashLeftException(CASH_LEFT_MESSAGE);
		}
		if (activeTicketPort.countActiveTickets(memberId) > 0) {
			throw new WithdrawTicketLeftException(TICKET_LEFT_MESSAGE);
		}

		member.withdraw(LocalDateTime.now());
		// CHECK 제약 위반 같은 DB 오류를 이 트랜잭션 안에서 드러내려고 바로 반영한다
		memberRepository.saveAndFlush(member);
		// 그 회원의 모든 기기 재발급을 막는다. Redis 오류면 예외가 나가 탈퇴도 롤백된다
		refreshTokenRepository.deleteAll(memberId);
		// 커밋된 뒤에만 Kafka로 나간다 (MemberKafkaPublisher, AFTER_COMMIT)
		member.publishWithdrawnEvent();
	}
}
