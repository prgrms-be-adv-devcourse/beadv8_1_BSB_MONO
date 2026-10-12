package com.bukang.order.domain;

import static jakarta.persistence.EnumType.STRING;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

import com.bukang.common.global.jpa.entity.BaseIdAndTime;
import com.bukang.order.domain.exception.ParticipationNotEditableException;
import com.bukang.order.domain.exception.ParticipationNotPayableException;
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
		@Index(name = "idx_participation_member", columnList = "member_id, id"),
		// 결제 기한이 지난 결제 대기 신청 찾기 (만료 정리 배치, PRO-42)
		@Index(name = "idx_participation_due", columnList = "status, payment_due_at")
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
		changeStatus(ParticipationStatus.PENDING);
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

	// [결제하기]에서 자리를 확보했다. 결제 기한은 상품 서비스가 정한 값의 사본이다
	public void markApplied(LocalDateTime paymentDueAt) {
		checkPayable();
		changeStatus(ParticipationStatus.APPLIED);
		this.paymentDueAt = paymentDueAt;
	}

	// 참가를 확정할 수 있는지: 결제 대기(APPLIED)여야 한다. 결제는 예치금을 차감하기 전에 이것부터 확인한다
	public void checkConfirmable() {
		if (status != ParticipationStatus.APPLIED) {
			throw new ParticipationNotPayableException(
				"결제 대기 중인 신청만 참가를 확정할 수 있습니다(현재 상태: %s).".formatted(status.getLabel()));
		}
	}

	// 결제를 마쳐 참가가 확정됐다 (정책 3장 "결제한 사람만 참가 확정")
	public void markConfirmed(LocalDateTime confirmedAt) {
		checkConfirmable();
		changeStatus(ParticipationStatus.CONFIRMED);
		this.confirmedAt = confirmedAt;
	}

	// 결제한 참가권을 취소했다. 끝난 신청이 되어 같은 대회에 다시 신청할 수 있다 (정책 5장)
	public void markCanceled(LocalDateTime canceledAt) {
		if (status != ParticipationStatus.CONFIRMED) {
			throw new ParticipationNotPayableException(
				"참가 확정된 신청만 취소할 수 있습니다(현재 상태: %s).".formatted(status.getLabel()));
		}
		changeStatus(ParticipationStatus.CANCELED);
		this.canceledAt = canceledAt;
	}

	// 결제 기한을 늘릴 수 있는지: 결제 대기(APPLIED)여야 한다. 상품 서비스에 연장을 요청하기 전에 확인한다
	public void checkExtendable() {
		if (status != ParticipationStatus.APPLIED) {
			throw new ParticipationNotPayableException(
				"결제 대기 중인 신청만 결제 기한을 늘릴 수 있습니다(현재 상태: %s).".formatted(status.getLabel()));
		}
	}

	// 결제 기한을 늘렸다 (PG 충전창 진입 때 2분, 한 번). 결제 대기(APPLIED) 신청만 늘린다
	public void extendPaymentDue(LocalDateTime newPaymentDueAt) {
		checkExtendable();
		this.paymentDueAt = newPaymentDueAt;
	}

	// 남은 자리가 없어 확보하지 못했다. 끝난 신청이 되어 같은 대회에 다시 신청할 수 있다
	public void markHoldFailed() {
		checkPayable();
		changeStatus(ParticipationStatus.FAILED);
	}

	public boolean isHoldFailed() {
		return status == ParticipationStatus.FAILED;
	}

	// [결제하기]는 자리 확보 전(PENDING) 신청만 할 수 있다. 상품 서비스를 부르기 전에 먼저 확인한다
	public void checkPayable() {
		if (status != ParticipationStatus.PENDING) {
			throw new ParticipationNotPayableException(
				"결제를 시작할 수 없는 신청입니다(현재 상태: %s).".formatted(status.getLabel()));
		}
	}

	// 상태와 active_flag는 늘 이 메서드로 함께 바꾼다. 끝난 상태면 active_flag를 비워 한 대회 1건 제약에서 빠진다
	private void changeStatus(ParticipationStatus newStatus) {
		this.status = newStatus;
		this.activeFlag = newStatus.isActive() ? Boolean.TRUE : null;
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
