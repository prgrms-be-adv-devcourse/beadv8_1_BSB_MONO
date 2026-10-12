package com.bukang.order.domain;

import static jakarta.persistence.EnumType.STRING;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 참가 신청서에 적는 참가자 정보 (정책 3장 1-2)
 * 접수 마감 전까지 통째로 바꿀 수 있고, 다음 신청 때 그대로 채워 준다
 * 컬럼은 크루원 행 때문에 NULL을 허용하고(ERD v4), 개인 신청의 필수 여부는 요청 DTO에서 검사한다
 */
@Embeddable
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor
@Getter
public class ParticipantInfo {
	@Column(name = "participant_name", length = 50)
	private String name;

	@Column(name = "participant_birth")
	private LocalDate birth;

	@Enumerated(STRING)
	@Column(name = "participant_gender", columnDefinition = "varchar(10)")
	private Gender gender;

	@Column(name = "participant_phone", length = 20)
	private String phone;

	@Column(name = "emergency_contact_name", length = 50)
	private String emergencyContactName;

	@Column(name = "emergency_contact_phone", length = 20)
	private String emergencyContactPhone;

	// 기념 티셔츠(키트) 사이즈. 종목마다 고를 수 있는 값이 달라 문자열로 둔다 (ERD v4 tshirt_size)
	@Column(name = "tshirt_size", length = 10)
	private String tshirtSize;
}
