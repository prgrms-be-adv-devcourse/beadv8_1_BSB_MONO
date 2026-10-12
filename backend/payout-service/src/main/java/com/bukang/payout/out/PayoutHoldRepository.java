package com.bukang.payout.out;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bukang.payout.domain.PayoutHold;

public interface PayoutHoldRepository extends JpaRepository<PayoutHold, Integer> {
}
