package com.bukang.member.app;

import java.time.LocalDate;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bukang.member.config.crypto.BlindIndexGenerator;
import com.bukang.member.domain.Gender;
import com.bukang.member.domain.Member;
import com.bukang.member.domain.exception.DuplicateEmailException;
import com.bukang.member.domain.exception.DuplicateNicknameException;
import com.bukang.member.domain.exception.DuplicatePhoneException;
import com.bukang.member.domain.exception.UnderAgeException;
import com.bukang.member.out.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberJoinUseCase {
	private static final String DUPLICATE_EMAIL_MESSAGE = "이미 사용 중인 이메일입니다.";
	private static final String DUPLICATE_NICKNAME_MESSAGE = "이미 사용 중인 닉네임입니다.";
	private static final String DUPLICATE_PHONE_MESSAGE = "이미 사용 중인 전화번호입니다.";
	// Member의 @UniqueConstraint 이름과 같아야 한다
	private static final String EMAIL_CONSTRAINT = "uk_member_email";
	private static final String NICKNAME_CONSTRAINT = "uk_member_nickname";
	private static final String PHONE_CONSTRAINT = "uk_member_phone_hash";

	private final MemberRepository memberRepository;
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
		Gender gender
	) {
		if (!Member.isJoinableAge(birthDate, LocalDate.now())) {
			throw new UnderAgeException("만 14세 미만은 가입할 수 없습니다.");
		}
		if (memberRepository.existsByEmail(email)) {
			throw new DuplicateEmailException(DUPLICATE_EMAIL_MESSAGE);
		}
		if (memberRepository.existsByNickname(nickname)) {
			throw new DuplicateNicknameException(DUPLICATE_NICKNAME_MESSAGE);
		}

		String phoneHash = blindIndexGenerator.generatePhone(phone);
		if (memberRepository.existsByPhoneHash(phoneHash)) {
			throw new DuplicatePhoneException(DUPLICATE_PHONE_MESSAGE);
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
		try {
			// 유니크 제약 위반을 여기서 잡으려고 바로 반영한다.
			// save만 하면 Facade의 트랜잭션이 커밋될 때 에러가 나서 이 메서드에서 잡을 수 없다
			return memberRepository.saveAndFlush(newMember);
		} catch (DataIntegrityViolationException exception) {
			// 같은 값으로 동시에 가입하면 둘 다 위의 중복 검사를 통과하고, 늦은 쪽이 DB 유니크 제약에 걸린다
			throw toDuplicateException(exception);
		}
	}

	// 제약 이름으로 어느 칸이 겹쳤는지 구별한다. 회원 제약이 아니면 원래 예외를 그대로 던진다
	private RuntimeException toDuplicateException(DataIntegrityViolationException exception) {
		String message = exception.getMostSpecificCause().getMessage();
		if (message == null) {
			return exception;
		}
		String lowerCaseMessage = message.toLowerCase();
		if (lowerCaseMessage.contains(NICKNAME_CONSTRAINT)) {
			return new DuplicateNicknameException(DUPLICATE_NICKNAME_MESSAGE);
		}
		if (lowerCaseMessage.contains(PHONE_CONSTRAINT)) {
			return new DuplicatePhoneException(DUPLICATE_PHONE_MESSAGE);
		}
		if (lowerCaseMessage.contains(EMAIL_CONSTRAINT)) {
			return new DuplicateEmailException(DUPLICATE_EMAIL_MESSAGE);
		}
		return exception;
	}
}
