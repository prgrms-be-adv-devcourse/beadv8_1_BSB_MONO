package com.bukang.cash.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bukang.cash.domain.payout.PayoutTransfer;
import com.bukang.cash.domain.payout.PayoutTransferType;

public interface PayoutTransferRepository extends JpaRepository<PayoutTransfer, Integer> {
	Optional<PayoutTransfer> findByPayoutIdAndTransferType(int payoutId, PayoutTransferType transferType);
}
