package com.bukang.cash.app;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.bukang.cash.domain.member.CashMember;
import com.bukang.cash.out.CashKafkaPublisher;
import com.bukang.cash.out.CashMemberRepository;
import com.bukang.common.shared.cash.event.CashMemberCreatedEvent;
import com.bukang.common.shared.kafka.KafkaTopics;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CashSyncMemberUseCase {
	private final CashMemberRepository cashMemberRepository;
	private final CashKafkaPublisher cashKafkaPublisher;

	public CashMember syncMember(int id, LocalDateTime createDate, LocalDateTime modifyDate,
		String nickname) {
		boolean isNew = !cashMemberRepository.existsById(id);

		CashMember cashMember = new CashMember(id, createDate, modifyDate, nickname);
		cashMemberRepository.save(cashMember);

		if (isNew) {
			cashKafkaPublisher.publish(
				KafkaTopics.CASH_MEMBER_CREATED,
				String.valueOf(cashMember.getId()),
				new CashMemberCreatedEvent(cashMember.getId())
			);
		}
		return cashMember;
	}
}
