package com.bukang.order.app;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.bukang.order.domain.ParticipantInfo;
import com.bukang.order.domain.Participation;
import com.bukang.order.domain.exception.AgeRestrictionException;
import com.bukang.order.domain.exception.ApplicationPeriodException;
import com.bukang.order.domain.exception.DuplicateParticipationException;
import com.bukang.order.domain.exception.ParticipationAccessDeniedException;
import com.bukang.order.domain.exception.ParticipationNotFoundException;
import com.bukang.order.domain.exception.RaceCourseNotFoundException;
import com.bukang.order.out.ParticipationRepository;
import com.bukang.order.out.race.RaceCourseInfo;
import com.bukang.order.out.race.RaceCoursePort;

import lombok.RequiredArgsConstructor;

/**
 * 참가 신청서를 받고 고친다 (정책 3장 1). 결제 전에 먼저 작성하고, 저장하면 PENDING(자리 확보 전)이다
 * 종목 정보(접수 기간, 나이 제한)는 상품 서비스(B)에서 받는다
 */
@Service
@RequiredArgsConstructor
public class ParticipationApplyUseCase {
	// Participation의 @UniqueConstraint 이름과 같아야 한다
	private static final String ACTIVE_PARTICIPATION_CONSTRAINT = "uk_participation_active";

	private final ParticipationRepository participationRepository;
	private final RaceCoursePort raceCoursePort;

	public Participation apply(int memberId, int raceCourseId, ParticipantInfo participant, LocalDateTime now) {
		RaceCourseInfo course = findCourse(raceCourseId);
		if (!course.isAccepting(now)) {
			throw new ApplicationPeriodException("접수 기간이 아닙니다.");
		}
		checkAge(course, participant);
		if (participationRepository.existsByRaceIdAndMemberIdAndActiveFlagTrue(course.raceId(), memberId)) {
			throw new DuplicateParticipationException("이미 이 대회에 신청했습니다.");
		}
		Participation participation = Participation.individual(
			memberId, course.raceId(), course.raceCourseId(), participant, now);
		try {
			// 위 확인과 저장 사이에 같은 신청이 먼저 들어오면 유니크 제약(uk_participation_active)이 막는다
			return participationRepository.saveAndFlush(participation);
		} catch (DataIntegrityViolationException exception) {
			// 다른 제약 위반까지 "이미 신청함"으로 바꾸면 진짜 원인이 가려지므로, 이 유니크 제약일 때만 바꾼다
			if (isActiveParticipationConflict(exception)) {
				throw new DuplicateParticipationException("이미 이 대회에 신청했습니다.");
			}
			throw exception;
		}
	}

	private boolean isActiveParticipationConflict(DataIntegrityViolationException exception) {
		String message = exception.getMostSpecificCause().getMessage();
		return message != null && message.toLowerCase().contains(ACTIVE_PARTICIPATION_CONSTRAINT);
	}

	// 참가자 정보는 접수 마감 전까지 고칠 수 있다 (정책 3장 1-5). 생년월일이 바뀔 수 있으니 나이 제한도 다시 본다
	public Participation changeParticipant(int participationId, int memberId, ParticipantInfo participant,
		LocalDateTime now) {
		Participation participation = findOwned(participationId, memberId);
		RaceCourseInfo course = findCourse(participation.getRaceCourseId());
		if (!course.isBeforeClose(now)) {
			throw new ApplicationPeriodException("접수가 마감되어 참가자 정보를 고칠 수 없습니다.");
		}
		checkAge(course, participant);
		participation.changeParticipant(participant);
		return participation;
	}

	public Participation findOwned(int participationId, int memberId) {
		Participation participation = participationRepository.findById(participationId)
			.orElseThrow(() -> new ParticipationNotFoundException("존재하지 않는 참가 신청입니다."));
		if (!participation.isOwnedBy(memberId)) {
			throw new ParticipationAccessDeniedException("본인의 참가 신청만 보거나 고칠 수 있습니다.");
		}
		return participation;
	}

	private RaceCourseInfo findCourse(int raceCourseId) {
		return raceCoursePort.findCourse(raceCourseId)
			.orElseThrow(() -> new RaceCourseNotFoundException("존재하지 않는 종목입니다."));
	}

	private void checkAge(RaceCourseInfo course, ParticipantInfo participant) {
		if (!course.allowsAge(participant.getBirth())) {
			throw new AgeRestrictionException("참가할 수 있는 나이가 아닙니다(대회 당일 %s, 신청자 만 %d세)."
				.formatted(course.ageRangeLabel(), course.ageOnRaceDay(participant.getBirth())));
		}
	}
}
