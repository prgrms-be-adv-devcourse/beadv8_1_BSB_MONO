package com.bukang.cash.domain.payout;

/**
 * 정산 지급 한 번의 이동 종류 (PRO-256)
 * 대금(SETTLEMENT)과 수수료(FEE)는 받는 지갑(판매자·시스템)이 서로 달라 별도 이동으로 처리한다.
 */
public enum PayoutTransferType {
	SETTLEMENT,
	FEE
}
