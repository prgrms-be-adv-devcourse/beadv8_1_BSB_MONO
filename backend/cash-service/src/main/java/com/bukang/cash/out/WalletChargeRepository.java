package com.bukang.cash.out;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bukang.cash.domain.WalletCharge;

public interface WalletChargeRepository extends JpaRepository<WalletCharge, Integer> {
}
