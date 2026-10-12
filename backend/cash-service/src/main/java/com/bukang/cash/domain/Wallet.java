package com.bukang.cash.domain;

import static jakarta.persistence.FetchType.LAZY;

import com.bukang.common.global.jpa.entity.BaseManualIdAndTime;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원별 예치금 지갑 (회원당 하나)
 * 1번 지갑은 system(플랫폼 수수료 수익), 2번 지갑은 holding(판매자 정산 전 보관)으로 쓴다.
 */
@Entity
@NoArgsConstructor
@Getter
@Table(name = "CASH_WALLET")
public class Wallet extends BaseManualIdAndTime {
	@ManyToOne(fetch = LAZY, optional = false)
	private CashMember holder;

	private long balance;
	@Version
	private long version;

	public Wallet(CashMember holder) {
		super(holder.getId());
		this.holder = holder;
	}
}
