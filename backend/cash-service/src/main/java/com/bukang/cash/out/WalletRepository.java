package com.bukang.cash.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bukang.cash.domain.wallet.Wallet;

public interface WalletRepository extends JpaRepository<Wallet, Integer> {
	Optional<Wallet> findByHolderId(int cashMemberId);
}
