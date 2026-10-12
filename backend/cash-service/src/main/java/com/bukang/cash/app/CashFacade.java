package com.bukang.cash.app;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.cash.domain.CashMember;
import com.bukang.cash.domain.Wallet;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CashFacade {
	private final CashSupport cashSupport;
	private final CashSyncMemberUseCase cashSyncMemberUseCase;
	private final CashCreateWalletUseCase cashCreateWalletUseCase;

	@Transactional
	public CashMember syncMember(int id, LocalDateTime createDate, LocalDateTime modifyDate,
		String username, String nickname) {
		return cashSyncMemberUseCase.syncMember(id, createDate, modifyDate, username, nickname);
	}

	@Transactional
	public Wallet createWallet(int id) {
		return cashCreateWalletUseCase.createWallet(id);
	}
}
