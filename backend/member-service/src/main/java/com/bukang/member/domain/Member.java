package com.bukang.member.domain;

import java.time.LocalDate;
import java.time.Period;

import com.bukang.common.shared.member.event.MemberJoinedEvent;
import com.bukang.common.shared.member.event.MemberModifiedEvent;
import com.bukang.member.dto.MemberDto;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Getter
// nickname은 BaseMember(복제본과 공유)에 있어 unique, 길이, NOT NULL을 원본 테이블에서만 건다
// 길이 16은 가장 긴 닉네임(영문·숫자 16자)에 맞춘다 (BE-013)
// unique 제약은 이름을 붙여, DB 중복 에러에서 어느 칸인지 구별한다 (BE-014, MemberJoinUseCase)
@Table(name = "MEMBER_MEMBER", uniqueConstraints = {
	@UniqueConstraint(name = "uk_member_email", columnNames = "email"),
	@UniqueConstraint(name = "uk_member_nickname", columnNames = "nickname"),
	@UniqueConstraint(name = "uk_member_phone_hash", columnNames = "phone_hash")
})
@AttributeOverride(name = "nickname", column = @Column(name = "nickname", nullable = false, length = 16))
public class Member extends SourceMember {
	private static final int MIN_JOIN_AGE = 14;
	// 닉네임 칸 수 범위. 한글 2자·영문 4자 ~ 한글 8자·영문 16자 (BE-013)
	public static final int NICKNAME_MIN_WIDTH = 4;
	public static final int NICKNAME_MAX_WIDTH = 16;

	// 관리자 판별용 역할 (BE-020). 칸을 더해도 dev DB를 초기화하지 않도록(ddl-auto: update) DB 기본값을 'USER'로 둔다
	// 이미 있는 행에는 칸을 더할 때 기본값이 채워진다
	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, columnDefinition = "varchar(10) default 'USER'")
	private MemberRole role = MemberRole.USER;

	public Member(
		String email,
		String password,
		String name,
		String nickname,
		String phone,
		String phoneHash,
		LocalDate birthDate,
		Gender gender
	) {
		super(email, password, name, nickname, phone, phoneHash, birthDate, gender);
	}

	public boolean isAdmin() {
		return role == MemberRole.ADMIN;
	}

	// 관리자 역할을 준다. 관리자 지정 API는 없고, 개발용 기본 데이터(MemberDataInit)에서만 쓴다
	public void grantAdmin() {
		role = MemberRole.ADMIN;
	}

	// 만 나이 기준. 생일 당일부터 한 살 더한다
	public static boolean isJoinableAge(LocalDate birthDate, LocalDate today) {
		return Period.between(birthDate, today).getYears() >= MIN_JOIN_AGE;
	}

	// 가입 이벤트를 발행한다. ID는 저장할 때(IDENTITY) 정해지므로 save() 뒤에 부른다
	public void publishJoinedEvent() {
		publishEvent(new MemberJoinedEvent(getId(), getCreateDate(), getModifyDate(), getNickname()));
	}

	// 수정 이벤트를 발행한다. 수정 시각은 DB에 반영(flush)할 때 채워지므로 saveAndFlush() 뒤에 부른다 (BE-015)
	public void publishModifiedEvent() {
		publishEvent(new MemberModifiedEvent(getId(), getCreateDate(), getModifyDate(), getNickname()));
	}

	// 한글 완성형(가~힣)은 화면에서 영문보다 두 배쯤 넓어 2칸, 그 밖의 글자는 1칸으로 센다 ("러너abc" = 7칸)
	public static int nicknameWidth(String nickname) {
		int width = 0;
		for (char character : nickname.toCharArray()) {
			width += (character >= '가' && character <= '힣') ? 2 : 1;
		}
		return width;
	}

	// 이름·닉네임·휴대폰·생년월일을 바꾸고, 하나라도 바뀌었으면 true를 돌려준다 (BE-012)
	// 휴대폰은 해시로 비교해, 하이픈 유무만 다른 같은 번호면 저장된 값을 그대로 둔다
	public boolean modify(String name, String nickname, String phone, String phoneHash, LocalDate birthDate) {
		boolean phoneChanged = !getPhoneHash().equals(phoneHash);
		boolean changed = phoneChanged
			|| !getName().equals(name)
			|| !getNickname().equals(nickname)
			|| !getBirthDate().equals(birthDate);
		if (!changed) {
			return false;
		}
		setName(name);
		setNickname(nickname);
		if (phoneChanged) {
			setPhone(phone);
			setPhoneHash(phoneHash);
		}
		setBirthDate(birthDate);
		return true;
	}

	public MemberDto toDto() {
		return new MemberDto(
			getId(),
			getCreateDate(),
			getModifyDate(),
			getNickname(),
			getEmail(),
			getPhone(),
			getName(),
			getBirthDate()
		);
	}
}
