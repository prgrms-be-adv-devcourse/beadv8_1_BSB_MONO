package com.bukang.cash.in;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.bukang.cash.app.CashFacade;
import com.bukang.cash.domain.event.CashTopics;
import com.bukang.common.global.json.JsonConverter;
import com.bukang.common.shared.cash.event.CashMemberCreatedEvent;
import com.bukang.common.shared.member.event.MemberJoinedEvent;
import com.bukang.common.shared.member.event.MemberModifiedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CashEventListener {
	private final JsonConverter jsonConverter;
	private final CashFacade cashFacade;

	@KafkaListener(topics = "queuing.member.joined", groupId = "CashEventListener__handleMemberJoined__1")
	public void handleMemberJoined(String message) {
		MemberJoinedEvent event = jsonConverter.fromJson(message, MemberJoinedEvent.class);
		cashFacade.syncMember(event.id(), event.createDate(), event.modifyDate(), event.nickname());
	}

	@KafkaListener(topics = "queuing.member.modified", groupId = "CashEventListener__handleMemberModified__1")
	public void handleMemberModified(String message) {
		MemberModifiedEvent event = jsonConverter.fromJson(message, MemberModifiedEvent.class);
		cashFacade.syncMember(event.id(), event.createDate(), event.modifyDate(), event.nickname());
	}

	@KafkaListener(topics = CashTopics.CASH_MEMBER_CREATED, groupId = "CashEventListener__handleCashMemberCreated__1")
	public void handleCashMemberCreated(String message) {
		CashMemberCreatedEvent event = jsonConverter.fromJson(message, CashMemberCreatedEvent.class);
		cashFacade.createWallet(event.id());
	}
}
