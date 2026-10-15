package com.bukang.cash.out.pg;

/**
 * PG(결제대행사)에 결제 승인을 요청한다 (PRO-81)
 * 프론트가 PG 결제창에서 받은 paymentKey·orderId·amount를 그대로 넘기면, 이 승인이 끝나야 실제로 돈이 움직인다.
 * 실패하면 PaymentApprovalFailedException을 던진다.
 */
public interface PaymentClient {
	PaymentApprovalResult approve(String paymentKey, String orderId, int amount);
}
