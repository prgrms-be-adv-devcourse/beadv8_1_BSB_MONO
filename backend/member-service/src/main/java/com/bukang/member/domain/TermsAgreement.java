package com.bukang.member.domain;

import java.time.LocalDateTime;

import com.bukang.common.global.jpa.entity.BaseIdAndTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 약관 동의 기록. 고치지 않고 쌓으므로 유일 제약이 없다 (BE-021)
@Entity
@NoArgsConstructor
@Getter
@Table(name = "MEMBER_TERMS_AGREEMENT")
public class TermsAgreement extends BaseIdAndTime {
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "terms_id", nullable = false)
	private Terms terms;

	@Column(nullable = false)
	private boolean agreed;

	@Column(nullable = false)
	private LocalDateTime decidedAt;

	public TermsAgreement(Member member, Terms terms, boolean agreed, LocalDateTime decidedAt) {
		this.member = member;
		this.terms = terms;
		this.agreed = agreed;
		this.decidedAt = decidedAt;
	}
}
