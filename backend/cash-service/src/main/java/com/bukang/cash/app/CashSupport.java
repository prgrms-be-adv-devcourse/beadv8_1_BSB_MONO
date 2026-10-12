package com.bukang.cash.app;

import org.springframework.stereotype.Service;

import com.bukang.cash.domain.Wallet;
import com.bukang.cash.domain.exception.WalletNotFoundException;
import com.bukang.cash.out.WalletRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CashSupport {
	private final WalletRepository walletRepository;

	// 지갑 ID = 회원 ID. 지갑은 가입 이벤트로 만들어지므로, 없으면 조회 시점에 만들지 않고 404로 알린다
	public Wallet findWalletByHolderId(int holderId) {
		return walletRepository.findById(holderId)
			.orElseThrow(() -> new WalletNotFoundException("존재하지 않는 지갑입니다."));
	}
}
