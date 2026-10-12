package com.bukang.cash.app;

import org.springframework.stereotype.Service;

import com.bukang.cash.domain.CashMember;
import com.bukang.cash.domain.Wallet;
import com.bukang.cash.out.CashMemberRepository;
import com.bukang.cash.out.WalletRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CashCreateWalletUseCase {
	private final WalletRepository walletRepository;
	private final CashMemberRepository cashMemberRepository;

	public Wallet createWallet(int cashMemberId) {
		return walletRepository.findByHolderId(cashMemberId)
			.orElseGet(() -> {
				CashMember member = cashMemberRepository.getReferenceById(cashMemberId);
				return walletRepository.save(new Wallet(member));
			});
	}
}
