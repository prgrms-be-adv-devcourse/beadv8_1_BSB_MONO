package com.bukang.member.domain;

import java.time.LocalDate;
import java.time.Period;

import com.bukang.common.shared.member.event.MemberJoinedEvent;
import com.bukang.member.dto.MemberDto;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

	// 만 나이 기준. 생일 당일부터 한 살 더한다
	public static boolean isJoinableAge(LocalDate birthDate, LocalDate today) {
		return Period.between(birthDate, today).getYears() >= MIN_JOIN_AGE;
	}

	// 가입 이벤트를 발행한다. ID는 저장할 때(IDENTITY) 정해지므로 save() 뒤에 부른다
	public void publishJoinedEvent() {
		publishEvent(new MemberJoinedEvent(getId(), getCreateDate(), getModifyDate(), getNickname()));
	}

	// 한글 완성형(가~힣)은 화면에서 영문보다 두 배쯤 넓어 2칸, 그 밖의 글자는 1칸으로 센다 ("러너abc" = 7칸)
	public static int nicknameWidth(String nickname) {
		int width = 0;
		for (char character : nickname.toCharArray()) {
			width += (character >= '가' && character <= '힣') ? 2 : 1;
		}
		return width;
	}

	public MemberDto toDto() {
		return new MemberDto(
			getId(),
			getCreateDate(),
			getModifyDate(),
			getNickname(),
			getEmail(),
			getPhone()
		);
	}
}
