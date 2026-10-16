package com.bukang.member.domain;

import java.time.LocalDate;

import com.bukang.common.global.jpa.entity.BaseIdAndTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 약관. 종류마다 버전별로 쌓고, 지금 버전은 시행일이 오늘 이전인 것 중 가장 최근이다 (BE-021)
@Entity
@NoArgsConstructor
@Getter
@Table(name = "MEMBER_TERMS", uniqueConstraints = {
	@UniqueConstraint(name = "uk_terms_type_version", columnNames = {"type", "version"})
})
public class Terms extends BaseIdAndTime {
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TermsType type;

	@Column(nullable = false, length = 20)
	private String version;

	// 필수 약관이면 true. 가입할 때 동의하지 않으면 가입할 수 없다
	@Column(nullable = false)
	private boolean required;

	// 약관 원문 주소 (프론트 /terms/...)
	@Column(nullable = false, length = 255)
	private String contentUrl;

	@Column(nullable = false)
	private LocalDate effectiveDate;

	public Terms(TermsType type, String version, boolean required, String contentUrl, LocalDate effectiveDate) {
		this.type = type;
		this.version = version;
		this.required = required;
		this.contentUrl = contentUrl;
		this.effectiveDate = effectiveDate;
	}
}
