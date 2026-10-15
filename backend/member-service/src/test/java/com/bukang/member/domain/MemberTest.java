package com.bukang.member.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberTest {
	private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

	@Test
	@DisplayName("만 14세 생일 당일부터 가입할 수 있다")
	void joinableOn14thBirthday() {
		assertThat(Member.isJoinableAge(LocalDate.of(2012, 10, 10), TODAY)).isTrue();
	}

	@Test
	@DisplayName("만 14세 생일 하루 전에는 가입할 수 없다")
	void notJoinableDayBefore14thBirthday() {
		assertThat(Member.isJoinableAge(LocalDate.of(2012, 10, 11), TODAY)).isFalse();
	}

	@Test
	@DisplayName("닉네임 칸 수는 한글 2칸, 영문·숫자 1칸으로 세고 섞으면 더한다")
	void nicknameWidthCountsKoreanAsTwo() {
		assertThat(Member.nicknameWidth("러너")).isEqualTo(4);
		assertThat(Member.nicknameWidth("러너abc")).isEqualTo(7);
		assertThat(Member.nicknameWidth("가나다라마바사아")).isEqualTo(16);
		assertThat(Member.nicknameWidth("가나다라마바사아자")).isEqualTo(18);
		assertThat(Member.nicknameWidth("abcdefghijklmnop")).isEqualTo(16);
		assertThat(Member.nicknameWidth("abcdefghijklmnopq")).isEqualTo(17);
	}
}
