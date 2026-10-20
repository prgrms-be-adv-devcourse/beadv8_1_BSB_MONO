package com.bukang.cash.in;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.bukang.cash.app.CashFacade;
import com.bukang.common.global.json.JsonConverter;
import com.bukang.common.shared.kafka.KafkaTopics;
import com.bukang.common.shared.member.event.MemberJoinedEvent;
import com.bukang.common.shared.member.event.MemberModifiedEvent;
import com.bukang.common.shared.seller.event.SellerChangedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CashEventListener {
	private final JsonConverter jsonConverter;
	private final CashFacade cashFacade;

	@KafkaListener(topics = KafkaTopics.MEMBER_MEMBER_JOINED, groupId = "CashEventListener__handleMemberJoined__1")
	public void handleMemberJoined(String message) {
		MemberJoinedEvent event = jsonConverter.fromJson(message, MemberJoinedEvent.class);
		cashFacade.syncMember(event.id(), event.createDate(), event.modifyDate(), event.nickname());
	}

	@KafkaListener(topics = KafkaTopics.MEMBER_MODIFIED, groupId = "CashEventListener__handleMemberModified__1")
	public void handleMemberModified(String message) {
		MemberModifiedEvent event = jsonConverter.fromJson(message, MemberModifiedEvent.class);
		cashFacade.syncMember(event.id(), event.createDate(), event.modifyDate(), event.nickname());
	}

	// 판매자 등록 신청·승인·반려 때마다 온다. 정산 지급 API가 받는 sellerId(신청서 ID)를 회원 ID로 바꿀 때 쓴다 (PRO-256)
	@KafkaListener(topics = KafkaTopics.SELLER_CHANGED, groupId = "CashEventListener__handleSellerChanged__1")
	public void handleSellerChanged(String message) {
		SellerChangedEvent event = jsonConverter.fromJson(message, SellerChangedEvent.class);
		cashFacade.syncSeller(event);
	}
}
