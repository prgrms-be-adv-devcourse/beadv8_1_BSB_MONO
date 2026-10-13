package com.bukang.order.app;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.order.dto.ParticipantInfoDto;
import com.bukang.order.dto.ParticipantInfoRequestDto;
import com.bukang.order.dto.ParticipationApplyRequestDto;
import com.bukang.order.dto.ParticipationDto;
import com.bukang.order.out.ParticipationRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ParticipationFacade {
	private final ParticipationApplyUseCase participationApplyUseCase;
	private final ParticipationRepository participationRepository;

	@Transactional
	public ParticipationDto apply(ParticipationApplyRequestDto request, int memberId) {
		return participationApplyUseCase.apply(
			memberId,
			request.getRaceCourseId(),
			request.getParticipant().toParticipantInfo(),
			LocalDateTime.now()
		).toDto();
	}

	@Transactional
	public ParticipationDto changeParticipant(int participationId, ParticipantInfoRequestDto request, int memberId) {
		return participationApplyUseCase.changeParticipant(
			participationId,
			memberId,
			request.toParticipantInfo(),
			LocalDateTime.now()
		).toDto();
	}

	@Transactional
	public ParticipationDto cancelBeforePayment(int participationId, int memberId) {
		return participationApplyUseCase.cancelBeforePayment(participationId, memberId, LocalDateTime.now()).toDto();
	}

	public ParticipationDto findById(int participationId, int memberId) {
		return participationApplyUseCase.findOwned(participationId, memberId).toDto();
	}

	// 다음 신청서에 채울 참가자 정보. 신청한 적이 없으면 빈 값이다 (정책 3장 1-2)
	public Optional<ParticipantInfoDto> findLatestParticipant(int memberId) {
		return participationRepository.findFirstByMemberIdOrderByIdDesc(memberId)
			.map(participation -> ParticipantInfoDto.from(participation.getParticipant()));
	}
}
