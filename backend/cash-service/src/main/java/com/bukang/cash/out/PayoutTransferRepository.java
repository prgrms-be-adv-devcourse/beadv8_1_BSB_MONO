package com.bukang.cash.out;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bukang.cash.domain.payout.PayoutTransfer;
import com.bukang.cash.domain.payout.PayoutTransferType;

public interface PayoutTransferRepository extends JpaRepository<PayoutTransfer, Integer> {
	boolean existsByPayoutIdAndTransferType(int payoutId, PayoutTransferType transferType);
}
