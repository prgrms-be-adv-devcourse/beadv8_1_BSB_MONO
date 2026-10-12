package com.bukang.order.domain;

import static jakarta.persistence.EnumType.STRING;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

import com.bukang.common.global.jpa.entity.BaseIdAndTime;
import com.bukang.order.domain.exception.ParticipationNotEditableException;
import com.bukang.order.dto.ParticipantInfoDto;
import com.bukang.order.dto.ParticipationDto;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 참가 신청 (사람 단위). 참가하려는 사람 1명 = 1행이고, 개인 신청과 크루원 신청이 같은 테이블을 쓴다 (ERD 추천안 v4 7쪽)
 * 결제 전에 신청서를 먼저 받아 PENDING으로 저장하고, [결제하기]에서 자리를 확보하면 APPLIED가 된다 (PRO-41)
 */
@Entity
@NoArgsConstructor(access = PROTECTED)
@Getter
@Table(
	name = "BOOKING_PARTICIPATION",
	uniqueConstraints = {
		// 한 사람은 한 대회에 살아 있는 신청 1건 (정책 1장 3-1). 끝난 신청은 active_flag가 NULL이라 걸리지 않는다
		@UniqueConstraint(name = "uk_participation_active", columnNames = {"race_id", "member_id", "active_flag"})
	},
	indexes = {
		// 내 신청 목록, 다음 신청 때 채울 최근 신청 찾기
		@Index(name = "idx_participation_member", columnList = "member_id, id")
	}
)
public class Participation extends BaseIdAndTime {
	@Column(name = "race_id", nullable = false)
	private int raceId;

	@Column(name = "race_course_id", nullable = false)
	private int raceCourseId;

	@Column(name = "member_id", nullable = false)
	private int memberId;

	// 크루원 신청만 채운다. 세미는 늘 NULL (최종 프로젝트)
	@Column(name = "crew_application_id")
	private Integer crewApplicationId;

	@Enumerated(STRING)
	@Column(name = "participation_type", nullable = false, columnDefinition = "varchar(20)")
	private ParticipationType participationType;

	@Enumerated(STRING)
	@Column(name = "status", nullable = false, columnDefinition = "varchar(20)")
	private ParticipationStatus status;

	// 살아 있는 신청은 TRUE, 끝난 신청(RELEASED·FAILED·CANCELED)은 NULL. 유니크 제약이 NULL을 서로 다른 값으로 본다
	@Column(name = "active_flag")
	private Boolean activeFlag;

	// 결제 기한 화면용 사본. 원본은 상품 서비스의 자리 확보(RACE_SEAT.expires_at)다 (PRO-41)
	@Column(name = "payment_due_at")
	private LocalDateTime paymentDueAt;

	@Embedded
	private ParticipantInfo participant;

	// 개인정보 제3자 제공 동의 (필수, 판매자가 빼거나 고칠 수 없음, 정책 3장 1-3)
	@Column(name = "third_party_consent")
	private Boolean thirdPartyConsent;

	@Column(name = "consented_at")
	private LocalDateTime consentedAt;

	@Column(name = "confirmed_at")
	private LocalDateTime confirmedAt;

	@Column(name = "released_at")
	private LocalDateTime releasedAt;

	@Column(name = "canceled_at")
	private LocalDateTime canceledAt;

	private Participation(int memberId, int raceId, int raceCourseId, ParticipantInfo participant,
		LocalDateTime consentedAt) {
		this.memberId = memberId;
		this.raceId = raceId;
		this.raceCourseId = raceCourseId;
		this.participationType = ParticipationType.INDIVIDUAL;
		this.status = ParticipationStatus.PENDING;
		this.activeFlag = Boolean.TRUE;
		this.participant = participant;
		this.thirdPartyConsent = Boolean.TRUE;
		this.consentedAt = consentedAt;
	}

	// 개인 신청서. 제3자 제공 동의를 받은 뒤에만 만든다 (요청 DTO가 검사)
	public static Participation individual(int memberId, int raceId, int raceCourseId, ParticipantInfo participant,
		LocalDateTime consentedAt) {
		return new Participation(memberId, raceId, raceCourseId, participant, consentedAt);
	}

	public boolean isOwnedBy(int targetMemberId) {
		return memberId == targetMemberId;
	}

	// 접수 마감 전인지는 호출하는 쪽이 종목 정보로 확인한다. 여기서는 끝난 신청인지만 막는다
	public void changeParticipant(ParticipantInfo newParticipant) {
		if (!status.isActive()) {
			throw new ParticipationNotEditableException(
				"참가자 정보를 고칠 수 없는 신청입니다(현재 상태: %s).".formatted(status.getLabel()));
		}
		this.participant = newParticipant;
	}

	public ParticipationDto toDto() {
		return new ParticipationDto(
			getId(),
			getCreateDate(),
			getModifyDate(),
			raceId,
			raceCourseId,
			memberId,
			participationType,
			status,
			ParticipantInfoDto.from(participant),
			thirdPartyConsent,
			consentedAt,
			paymentDueAt
		);
	}
}
