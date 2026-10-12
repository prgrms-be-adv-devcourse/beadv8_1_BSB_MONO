package com.bukang.cash.out;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bukang.cash.domain.WalletLedger;

public interface WalletLedgerRepository extends JpaRepository<WalletLedger, Integer> {
}
