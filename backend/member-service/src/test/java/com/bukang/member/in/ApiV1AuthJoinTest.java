package com.bukang.member.in;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.bukang.member.app.MemberFacade;
import com.bukang.member.dto.MemberJoinRequestDto;
import com.bukang.member.out.MemberRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiV1AuthJoinTest {
	private static final String EMAIL = "runner1@crewrun.com";
	private static final String NICKNAME = "러너1";
	private static final String PHONE = "010-1234-5678";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberFacade memberFacade;

	// 동시 가입을 재현하려고 중복 검사(existsBy...)만 바꿔 치고, 저장은 진짜 DB(H2)로 한다
	@MockitoSpyBean
	private MemberRepository memberRepository;

	@AfterEach
	void tearDown() {
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("가입하면 201과 회원 정보를 받는다")
	void joinSucceeds() throws Exception {
		join(EMAIL, NICKNAME, PHONE)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value(201))
			.andExpect(jsonPath("$.data.nickname").value(NICKNAME))
			.andExpect(jsonPath("$.data.email").value(EMAIL));
	}

	@ParameterizedTest
	@ValueSource(strings = {"러너", "가나다라마바사아", "abcd", "abcdefghijklmnop", "러너abc", "Runner2026"})
	@DisplayName("닉네임이 4~16칸(한글 2칸, 영문·숫자 1칸)이면 가입할 수 있다")
	void joinSucceedsWithValidNickname(String nickname) throws Exception {
		join(EMAIL, nickname, PHONE)
			.andExpect(status().isCreated());
	}

	@ParameterizedTest
	@ValueSource(strings = {"러", "abc", "가나다라마바사아자", "abcdefghijklmnopq"})
	@DisplayName("닉네임이 4칸보다 짧거나 16칸보다 길면 400이다")
	void joinFailsWithNicknameOutOfWidth(String nickname) throws Exception {
		join(EMAIL, nickname, PHONE)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("닉네임은 한글 2~8자, 영문 4~16자로 작성해야합니다."));
	}

	@ParameterizedTest
	@ValueSource(strings = {"러너 1", "러너!", "러너😀", "ㅋㅋㅋㅋ", "runner_1"})
	@DisplayName("닉네임에 공백·특수문자·이모지·자음/모음만 있는 글자가 있으면 400이다")
	void joinFailsWithNicknameInvalidCharacters(String nickname) throws Exception {
		join(EMAIL, nickname, PHONE)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("닉네임은 한글, 영문, 숫자만 쓸 수 있습니다."));
	}

	@Test
	@DisplayName("닉네임 'ㅋㅋㅋ'는 400이다")
	void joinFailsWithConsonantOnlyNickname() throws Exception {
		// 문자 규칙과 칸 수(3칸) 둘 다 어겨 어느 메시지가 먼저 나올지 정해져 있지 않으므로 상태만 확인한다
		join(EMAIL, "ㅋㅋㅋ", PHONE)
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("이미 있는 닉네임으로 가입하면 409다")
	void joinFailsWithExistingNickname() throws Exception {
		joinDirectly(EMAIL, NICKNAME, PHONE);

		join("runner2@crewrun.com", NICKNAME, "010-9999-8888")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("이미 사용 중인 닉네임입니다."));
	}

	@Test
	@DisplayName("이미 있는 전화번호로 가입하면 409다")
	void joinFailsWithExistingPhone() throws Exception {
		joinDirectly(EMAIL, NICKNAME, PHONE);

		join("runner2@crewrun.com", "러너2", PHONE)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("이미 사용 중인 전화번호입니다."));
	}

	@Test
	@DisplayName("이미 있는 이메일로 가입하면 409다")
	void joinFailsWithExistingEmail() throws Exception {
		joinDirectly(EMAIL, NICKNAME, PHONE);

		join(EMAIL, "러너2", "010-9999-8888")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
	}

	@Test
	@DisplayName("동시 가입으로 중복 검사를 통과해도 DB 닉네임 제약에 걸리면 409다")
	void joinRaceFailsOnNicknameConstraint() throws Exception {
		joinDirectly(EMAIL, NICKNAME, PHONE);
		skipDuplicateChecks();

		join("runner2@crewrun.com", NICKNAME, "010-9999-8888")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("이미 사용 중인 닉네임입니다."));
	}

	@Test
	@DisplayName("동시 가입으로 중복 검사를 통과해도 DB 전화번호 제약에 걸리면 409다")
	void joinRaceFailsOnPhoneConstraint() throws Exception {
		joinDirectly(EMAIL, NICKNAME, PHONE);
		skipDuplicateChecks();

		join("runner2@crewrun.com", "러너2", PHONE)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("이미 사용 중인 전화번호입니다."));
	}

	@Test
	@DisplayName("동시 가입으로 중복 검사를 통과해도 DB 이메일 제약에 걸리면 409다")
	void joinRaceFailsOnEmailConstraint() throws Exception {
		joinDirectly(EMAIL, NICKNAME, PHONE);
		skipDuplicateChecks();

		join(EMAIL, "러너2", "010-9999-8888")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
	}

	// 먼저 가입한 회원이 아직 없다고 답하게 해, 두 요청이 동시에 중복 검사를 통과한 상황을 만든다
	private void skipDuplicateChecks() {
		willReturn(false).given(memberRepository).existsByEmail(anyString());
		willReturn(false).given(memberRepository).existsByNickname(anyString());
		willReturn(false).given(memberRepository).existsByPhoneHash(anyString());
	}

	private void joinDirectly(String email, String nickname, String phone) {
		memberFacade.join(new MemberJoinRequestDto(
			email, "password1234", "김러너", nickname, phone, LocalDate.of(1995, 4, 12), null
		));
	}

	private ResultActions join(String email, String nickname, String phone) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/join")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
					"email": "%s",
					"password": "password1234",
					"name": "김러너",
					"nickname": "%s",
					"phone": "%s",
					"birthDate": "1995-04-12"
				}
				""".formatted(email, nickname, phone)));
	}
}
