package com.bukang.order.app;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.bukang.order.domain.Order;
import com.bukang.order.domain.ParticipantInfo;
import com.bukang.order.domain.Participation;
import com.bukang.order.domain.exception.AgeRestrictionException;
import com.bukang.order.domain.exception.ApplicationPeriodException;
import com.bukang.order.domain.exception.DuplicateParticipationException;
import com.bukang.order.domain.exception.ParticipationAccessDeniedException;
import com.bukang.order.domain.exception.ParticipationChangeRetryException;
import com.bukang.order.domain.exception.ParticipationNotFoundException;
import com.bukang.order.domain.exception.RaceCourseNotFoundException;
import com.bukang.order.domain.exception.ShippingAddressRequiredException;
import com.bukang.order.domain.exception.TshirtSizeNotAvailableException;
import com.bukang.order.out.OrderItemRepository;
import com.bukang.order.out.OrderRepository;
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
	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final RaceCoursePort raceCoursePort;

	public Participation apply(int memberId, int raceCourseId, ParticipantInfo participant, LocalDateTime now) {
		RaceCourseInfo course = findCourse(raceCourseId);
		checkNotCanceled(course);
		if (!course.isAccepting(now)) {
			throw new ApplicationPeriodException("접수 기간이 아닙니다.");
		}
		checkAge(course, participant);
		ParticipantInfo checked = checkTshirtSize(course, checkShippingAddress(course, participant));
		if (participationRepository.existsByRaceIdAndMemberIdAndActiveFlagTrue(course.raceId(), memberId)) {
			throw new DuplicateParticipationException("이미 이 대회에 신청했습니다.");
		}
		Participation participation = Participation.individual(
			memberId, course.raceId(), course.raceCourseId(), checked, now);
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
		// [결제하기]로 주문이 있으면 주문의 배송지도 바꿔야 하므로 주문부터 잠근다 (잠금 순서: 주문 → 참가 신청)
		Optional<Integer> orderId = orderItemRepository.findOrderIdByParticipationId(participationId);
		Optional<Order> order = orderId.flatMap(orderRepository::findByIdForUpdate);
		// 잠가서 읽는다. 엔티티는 저장할 때 모든 컬럼을 다시 쓰므로, 잠그지 않으면 결제·취소가 바꾼 상태를
		// 옛 값으로 덮을 수 있다. 결제·취소도 참가 신청을 잠그고 바꾼다
		Participation participation = participationRepository.findByIdForUpdate(participationId)
			.orElseThrow(() -> new ParticipationNotFoundException("존재하지 않는 참가 신청입니다."));
		checkOwner(participation, memberId);
		// 주문을 찾은 뒤 신청서를 잠그기 전에 [결제하기]가 주문을 만들었으면, 그 주문에는 옛 배송지가 복사돼 있다
		// 신청서를 잡은 채 주문을 잠그면 결제(주문 → 참가 신청)와 서로 기다릴 수 있으므로, 거절하고 다시 시도하게 한다
		if (orderId.isEmpty() && orderItemRepository.findOrderIdByParticipationId(participationId).isPresent()) {
			throw new ParticipationChangeRetryException("방금 주문이 만들어졌습니다. 잠시 뒤 다시 시도해주세요.");
		}
		RaceCourseInfo course = findCourse(participation.getRaceCourseId());
		checkNotCanceled(course);
		if (!course.isBeforeClose(now)) {
			throw new ApplicationPeriodException("접수가 마감되어 참가자 정보를 고칠 수 없습니다.");
		}
		checkAge(course, participant);
		ParticipantInfo checked = checkTshirtSize(course, checkShippingAddress(course, participant));
		participation.changeParticipant(checked);
		order.ifPresent(found -> found.changeShippingAddress(checked.getShippingAddress()));
		return participation;
	}

	// [결제하기] 전 신청서를 버린다 (PRO-122). [결제하기]도 신청서를 잠그고 PENDING인지 보므로, 동시에 들어오면 한쪽만 반영된다
	public Participation cancelBeforePayment(int participationId, int memberId, LocalDateTime now) {
		Participation participation = participationRepository.findByIdForUpdate(participationId)
			.orElseThrow(() -> new ParticipationNotFoundException("존재하지 않는 참가 신청입니다."));
		checkOwner(participation, memberId);
		participation.cancelBeforePayment(now);
		return participation;
	}

	public Participation findOwned(int participationId, int memberId) {
		Participation participation = participationRepository.findById(participationId)
			.orElseThrow(() -> new ParticipationNotFoundException("존재하지 않는 참가 신청입니다."));
		checkOwner(participation, memberId);
		return participation;
	}

	private void checkOwner(Participation participation, int memberId) {
		if (!participation.isOwnedBy(memberId)) {
			throw new ParticipationAccessDeniedException("본인의 참가 신청만 보거나 고칠 수 있습니다.");
		}
	}

	private RaceCourseInfo findCourse(int raceCourseId) {
		return raceCoursePort.findCourse(raceCourseId)
			.orElseThrow(() -> new RaceCourseNotFoundException("존재하지 않는 종목입니다."));
	}

	// 키트는 배송으로만 받으므로 키트가 있는 종목은 배송 주소가 있어야 한다 (정책 2장 1-3, 3장 1-2, 5장 3-2)
	private ParticipantInfo checkShippingAddress(RaceCourseInfo course, ParticipantInfo participant) {
		if (!course.kitIncluded()) {
			return participant.withShippingAddress(null);
		}
		if (participant.getShippingAddress() == null) {
			throw new ShippingAddressRequiredException("키트를 받을 배송 주소를 작성해주세요.");
		}
		return participant;
	}

	// 티셔츠를 주는 종목은 사이즈를 종목이 주는 사이즈 중에서 골라야 한다 (10/14 김호빈님과 정함)
	// 티셔츠가 없는 종목은 보낸 사이즈를 남기지 않는다 (배송 주소와 같은 방식)
	private ParticipantInfo checkTshirtSize(RaceCourseInfo course, ParticipantInfo participant) {
		if (!course.offersTshirt()) {
			return participant.withTshirtSize(null);
		}
		String sizes = String.join(", ", course.tshirtSizeOption());
		if (participant.getTshirtSize() == null) {
			throw new TshirtSizeNotAvailableException("티셔츠 사이즈를 골라주세요(고를 수 있는 사이즈: %s).".formatted(sizes));
		}
		if (!course.tshirtSizeOption().contains(participant.getTshirtSize())) {
			throw new TshirtSizeNotAvailableException("이 종목에서 고를 수 없는 티셔츠 사이즈입니다(고를 수 있는 사이즈: %s)."
				.formatted(sizes));
		}
		return participant;
	}

	private void checkAge(RaceCourseInfo course, ParticipantInfo participant) {
		if (!course.allowsAge(participant.getBirth())) {
			throw new AgeRestrictionException("참가할 수 있는 나이가 아닙니다(대회 당일 %s, 신청자 만 %d세)."
				.formatted(course.ageRangeLabel(), course.ageOnRaceDay(participant.getBirth())));
		}
	}

	// 취소된 대회는 접수 기간 문구 대신 취소됐다고 알린다 (취소된 대회는 isAccepting·isBeforeClose도 거짓이다)
	static void checkNotCanceled(RaceCourseInfo course) {
		if (course.canceled()) {
			throw ApplicationPeriodException.raceCanceled("취소된 대회입니다.");
		}
	}
}
