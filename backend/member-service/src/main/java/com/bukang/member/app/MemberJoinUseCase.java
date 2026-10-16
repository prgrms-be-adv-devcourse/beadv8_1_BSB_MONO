package com.bukang.member.app;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bukang.member.config.crypto.BlindIndexGenerator;
import com.bukang.member.domain.Gender;
import com.bukang.member.domain.Member;
import com.bukang.member.domain.Terms;
import com.bukang.member.domain.TermsAgreement;
import com.bukang.member.domain.TermsType;
import com.bukang.member.domain.exception.DuplicateEmailException;
import com.bukang.member.domain.exception.DuplicateNicknameException;
import com.bukang.member.domain.exception.DuplicatePhoneException;
import com.bukang.member.domain.exception.RequiredTermsNotAgreedException;
import com.bukang.member.domain.exception.UnderAgeException;
import com.bukang.member.out.MemberRepository;
import com.bukang.member.out.TermsAgreementRepository;
import com.bukang.member.out.TermsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberJoinUseCase {
	private final MemberRepository memberRepository;
	private final TermsRepository termsRepository;
	private final TermsAgreementRepository termsAgreementRepository;
	private final PasswordEncoder passwordEncoder;
	private final BlindIndexGenerator blindIndexGenerator;

	// 예외 메시지에는 입력값(개인정보)을 넣지 않는다 (응답과 로그에 평문이 남지 않도록)
	public Member join(
		String email,
		String password,
		String name,
		String nickname,
		String phone,
		LocalDate birthDate,
		Gender gender,
		List<TermsType> agreedTerms
	) {
		if (!Member.isJoinableAge(birthDate, LocalDate.now())) {
			throw new UnderAgeException("만 14세 미만은 가입할 수 없습니다.");
		}

		// 지금 버전의 약관 가운데 필수 약관이 빠졌으면 막는다 (입력값 검사라 중복 검사보다 먼저 한다)
		List<Terms> currentTerms = termsRepository.findCurrent(LocalDate.now());
		boolean requiredMissing = currentTerms.stream()
			.anyMatch(terms -> terms.isRequired() && !agreedTerms.contains(terms.getType()));
		if (requiredMissing) {
			throw new RequiredTermsNotAgreedException("필수 약관에 동의해야 가입할 수 있습니다.");
		}

		if (memberRepository.existsByEmail(email)) {
			throw new DuplicateEmailException(MemberDuplicateExceptions.DUPLICATE_EMAIL_MESSAGE);
		}
		if (memberRepository.existsByNickname(nickname)) {
			throw new DuplicateNicknameException(MemberDuplicateExceptions.DUPLICATE_NICKNAME_MESSAGE);
		}

		String phoneHash = blindIndexGenerator.generatePhone(phone);
		if (memberRepository.existsByPhoneHash(phoneHash)) {
			throw new DuplicatePhoneException(MemberDuplicateExceptions.DUPLICATE_PHONE_MESSAGE);
		}

		Member newMember = new Member(
			email,
			passwordEncoder.encode(password),
			name,
			nickname,
			phone,
			phoneHash,
			birthDate,
			gender
		);
		Member savedMember;
		try {
			// 유니크 제약 위반을 여기서 잡으려고 바로 반영한다.
			// save만 하면 Facade의 트랜잭션이 커밋될 때 에러가 나서 이 메서드에서 잡을 수 없다
			savedMember = memberRepository.saveAndFlush(newMember);
		} catch (DataIntegrityViolationException exception) {
			// 같은 값으로 동시에 가입하면 둘 다 위의 중복 검사를 통과하고, 늦은 쪽이 DB 유니크 제약에 걸린다
			throw MemberDuplicateExceptions.from(exception);
		}

		// 지금 버전의 모든 약관에 대해 동의 여부를 한 줄씩 남긴다 (목록에 있으면 동의, 없으면 미동의)
		LocalDateTime decidedAt = LocalDateTime.now();
		termsAgreementRepository.saveAll(currentTerms.stream()
			.map(terms -> new TermsAgreement(savedMember, terms, agreedTerms.contains(terms.getType()), decidedAt))
			.toList());

		savedMember.publishJoinedEvent();
		return savedMember;
	}
}
