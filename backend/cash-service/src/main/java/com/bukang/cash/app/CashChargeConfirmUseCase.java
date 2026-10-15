package com.bukang.cash.app;

import com.bukang.cash.domain.charge.ChargeStatus;
import com.bukang.cash.domain.charge.WalletCharge;
import com.bukang.cash.domain.exception.ChargeAmountTooSmallException;
import com.bukang.cash.domain.exception.PaymentApprovalFailedException;
import com.bukang.cash.domain.wallet.LedgerType;
import com.bukang.cash.domain.wallet.Wallet;
import com.bukang.cash.domain.wallet.WalletLedger;
import com.bukang.cash.dto.CashChargeResponseDto;
import com.bukang.cash.out.WalletChargeRepository;
import com.bukang.cash.out.WalletLedgerRepository;
import com.bukang.cash.out.pg.PaymentApprovalResult;
import com.bukang.cash.out.pg.PaymentClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CashChargeConfirmUseCase {
    private static final int MINIMUM_AMOUNT = 10_000;

    private final CashSupport cashSupport;
    private final WalletChargeRepository walletChargeRepository;
    private final WalletLedgerRepository walletLedgerRepository;
    private final PaymentClient paymentClient;

    public CashChargeResponseDto confirmWalletCharge(int customerId, String paymentKey, String orderId, int amount) {
        Wallet customerWallet = cashSupport.findWalletByHolderId(customerId);

        WalletCharge existing = walletChargeRepository.findByIdempotencyKey(orderId)
                .orElse(null);
        if (existing != null) {
            return toDto(existing, customerWallet.getBalance());
        }

        if (amount < MINIMUM_AMOUNT) {
            throw new ChargeAmountTooSmallException("최소 충전 금액은 10,000원입니다.");
        }

        WalletCharge charge = walletChargeRepository.save(new WalletCharge(customerWallet, amount, "TOSS", orderId));

        PaymentApprovalResult result;
        try {
            result = paymentClient.approve(paymentKey, orderId, amount);
        } catch (PaymentApprovalFailedException e) {
            charge.markFailed();
            return CashChargeResponseDto.failed(customerWallet.getBalance());
        }


        charge.markCompleted(result.pgPaymentKey(), LocalDateTime.now());
        walletLedgerRepository.save(customerWallet.credit(
                result.approvedAmount(),
                LedgerType.충전__PG결제_토스페이먼츠,
                charge
        ));
        return CashChargeResponseDto.completed(customerWallet.getBalance());
    }

    private CashChargeResponseDto toDto(WalletCharge charge, int balance) {
        return charge.getStatus() == ChargeStatus.COMPLETED
                ? CashChargeResponseDto.completed(balance)
                : CashChargeResponseDto.failed(balance);
    }

}
