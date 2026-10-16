package com.bukang.member.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.bukang.common.shared.member.event.MemberModifiedEvent;
import com.bukang.member.app.MemberFacade;
import com.bukang.member.domain.Gender;
import com.bukang.member.domain.Member;
import com.bukang.member.domain.MemberStatus;
import com.bukang.member.domain.exception.WithdrawCheckFailedException;
import com.bukang.member.dto.MemberJoinRequestDto;
import com.bukang.member.out.MemberRepository;
import com.bukang.member.out.RefreshTokenRepository;
import com.bukang.member.out.cash.CashBalancePort;
import com.bukang.member.out.cash.HttpCashBalanceAdapter;
import com.bukang.member.out.order.ActiveTicketPort;
import com.bukang.member.out.order.HttpActiveTicketAdapter;

// 예치금·주문 서비스와 Redis는 띄우지 않으므로 포트와 Refresh Token 저장소를 목으로 바꾼다
// 수정 이벤트는 스프링 이벤트로 발행됐는지만 본다. Kafka로 나가는 건 MemberKafkaPublisherTest가 확인한다
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RecordApplicationEvents
class ApiV1MemberWithdrawTest {
	private static final String ME = "/api/v1/member/members/me";
	private static final String EMAIL = "runner1@crewrun.com";
	private static final String PASSWORD = "password1234";
	private static final String NICKNAME = "러너1";
	private static final String PHONE = "010-1234-5678";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberFacade memberFacade;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ApplicationEvents applicationEvents;

	@MockitoBean
	private CashBalancePort cashBalancePort;

	@MockitoBean
	private ActiveTicketPort activeTicketPort;

	@MockitoBean
	private RefreshTokenRepository refreshTokenRepository;

	private int memberId;

	@BeforeEach
	void setUp() {
		memberId = join(EMAIL, NICKNAME, PHONE);
		// 목의 기본값(0)이라 따로 정하지 않으면 남은 예치금·참가권이 없다
	}

	@AfterEach
	void tearDown() {
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("남은 예치금·참가권이 없으면 200이고, 상태가 WITHDRAWN이 되며 개인정보 칸이 비고 Refresh Token을 지운다")
	void withdrawSucceeds() throws Exception {
		withdraw(memberId)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("탈퇴했습니다."));

		Member withdrawn = memberRepository.findById(memberId).orElseThrow();
		assertThat(withdrawn.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
		assertThat(withdrawn.getWithdrawnAt()).isNotNull();
		assertThat(withdrawn.getEmail()).isNull();
		assertThat(withdrawn.getName()).isNull();
		assertThat(withdrawn.getNickname()).isNull();
		assertThat(withdrawn.getPhone()).isNull();
		assertThat(withdrawn.getPhoneHash()).isNull();
		assertThat(withdrawn.getBirthDate()).isNull();
		assertThat(withdrawn.getGender()).isNull();
		verify(refreshTokenRepository).deleteAll(memberId);
	}

	@Test
	@DisplayName("탈퇴하면 닉네임 \"탈퇴한 회원\"과 탈퇴 시각을 담은 수정 이벤트를 한 번 발행한다")
	void withdrawPublishesModifiedEvent() throws Exception {
		withdraw(memberId).andExpect(status().isOk());

		Member withdrawn = memberRepository.findById(memberId).orElseThrow();
		assertThat(modifiedEvents())
			.singleElement()
			.satisfies(event -> {
				assertThat(event.id()).isEqualTo(memberId);
				assertThat(event.nickname()).isEqualTo(Member.WITHDRAWN_NICKNAME);
				assertThat(event.modifyDate()).isEqualTo(withdrawn.getWithdrawnAt());
				assertThat(event.createDate()).isNotNull();
			});
	}

	@Test
	@DisplayName("남은 예치금이 있으면 409 MEMBER_WITHDRAW_CASH_LEFT이고, 참가권은 묻지 않으며 아무것도 바뀌지 않는다")
	void withdrawFailsWithCashLeft() throws Exception {
		given(cashBalancePort.findBalance(memberId)).willReturn(1_000);

		withdraw(memberId)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MEMBER_WITHDRAW_CASH_LEFT"));

		verify(activeTicketPort, never()).countActiveTickets(anyInt());
		assertNothingChanged();
	}

	@Test
	@DisplayName("끝나지 않은 참가권이 있으면 409 MEMBER_WITHDRAW_TICKET_LEFT이고 아무것도 바뀌지 않는다")
	void withdrawFailsWithTicketLeft() throws Exception {
		given(activeTicketPort.countActiveTickets(memberId)).willReturn(1);

		withdraw(memberId)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MEMBER_WITHDRAW_TICKET_LEFT"));

		assertNothingChanged();
	}

	@Test
	@DisplayName("예치금이나 참가권을 확인하지 못하면 503 MEMBER_WITHDRAW_CHECK_FAILED이고 아무것도 바뀌지 않는다")
	void withdrawFailsWhenCheckFails() throws Exception {
		willThrow(new WithdrawCheckFailedException(HttpCashBalanceAdapter.UNAVAILABLE_MESSAGE))
			.given(cashBalancePort).findBalance(memberId);

		withdraw(memberId)
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.code").value("MEMBER_WITHDRAW_CHECK_FAILED"))
			.andExpect(jsonPath("$.message").value(HttpCashBalanceAdapter.UNAVAILABLE_MESSAGE));
		assertNothingChanged();

		willThrow(new WithdrawCheckFailedException(HttpActiveTicketAdapter.UNAVAILABLE_MESSAGE))
			.given(activeTicketPort).countActiveTickets(memberId);
		// 예외를 던지게 해 둔 목을 다시 정하므로, 호출 없이 정하는 will...given 형태를 쓴다
		willReturn(0).given(cashBalancePort).findBalance(memberId);

		withdraw(memberId)
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.code").value("MEMBER_WITHDRAW_CHECK_FAILED"));
		assertNothingChanged();
	}

	@Test
	@DisplayName("이미 탈퇴한 회원이 다시 탈퇴하면 404 MEMBER_NOT_FOUND이고 다른 서비스에 묻지 않는다")
	void withdrawTwiceIsNotFound() throws Exception {
		withdraw(memberId).andExpect(status().isOk());

		withdraw(memberId)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
		// 첫 탈퇴 때 한 번만 물었다
		verify(cashBalancePort).findBalance(memberId);
		assertThat(modifiedEvents()).hasSize(1);
	}

	@Test
	@DisplayName("탈퇴한 회원의 내 정보 조회·수정은 404다")
	void withdrawnMemberIsNotFound() throws Exception {
		withdraw(memberId).andExpect(status().isOk());

		mockMvc.perform(get(ME).header(MemberIdHeader.NAME, memberId))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
		mockMvc.perform(put(ME)
				.header(MemberIdHeader.NAME, memberId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{ "name": "김러너", "nickname": "새닉네임", "phone": "010-2222-3333", "birthDate": "1995-04-12" }
					"""))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
	}

	@Test
	@DisplayName("탈퇴한 회원의 이메일·비밀번호로는 로그인할 수 없다")
	void withdrawnMemberCannotLogin() throws Exception {
		withdraw(memberId).andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{ "email": "%s", "password": "%s" }
					""".formatted(EMAIL, PASSWORD)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("MEMBER_LOGIN_FAILED"));
		verify(refreshTokenRepository, never()).create(anyLong(), any());
	}

	@Test
	@DisplayName("탈퇴 뒤 같은 이메일·닉네임·휴대폰 번호로 다시 가입할 수 있고, 탈퇴한 줄이 여럿이어도 unique에 걸리지 않는다")
	void rejoinAfterWithdraw() throws Exception {
		withdraw(memberId).andExpect(status().isOk());

		int rejoinedId = join(EMAIL, NICKNAME, PHONE);
		assertThat(rejoinedId).isNotEqualTo(memberId);
		assertThat(memberRepository.findActiveByEmail(EMAIL)).hasValueSatisfying(member ->
			assertThat(member.getId()).isEqualTo(rejoinedId));

		// 다시 가입한 회원도 탈퇴하면 개인정보가 NULL인 줄이 둘이 된다
		withdraw(rejoinedId).andExpect(status().isOk());
		assertThat(memberRepository.findAll())
			.extracting(Member::getStatus)
			.containsOnly(MemberStatus.WITHDRAWN);
	}

	@Test
	@DisplayName("탈퇴하지 않은 회원의 개인정보 칸을 비우면 DB CHECK 제약에 걸린다")
	void activeMemberPersonalInfoCheck() {
		assertThatThrownBy(() -> jdbcTemplate.update(
			"update member_member set email = null where member_id = ?", memberId))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	private void assertNothingChanged() {
		Member member = memberRepository.findById(memberId).orElseThrow();
		assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
		assertThat(member.getWithdrawnAt()).isNull();
		assertThat(member.getEmail()).isEqualTo(EMAIL);
		assertThat(member.getNickname()).isEqualTo(NICKNAME);
		assertThat(member.getPhone()).isEqualTo(PHONE);
		assertThat(member.getGender()).isEqualTo(Gender.FEMALE);
		verify(refreshTokenRepository, never()).deleteAll(anyLong());
		assertThat(modifiedEvents()).isEmpty();
	}

	private List<MemberModifiedEvent> modifiedEvents() {
		return applicationEvents.stream(MemberModifiedEvent.class).toList();
	}

	private int join(String email, String nickname, String phone) {
		return memberFacade.join(new MemberJoinRequestDto(
			email, PASSWORD, "김러너", nickname, phone, LocalDate.of(1995, 4, 12), Gender.FEMALE
		)).getId();
	}

	private ResultActions withdraw(int id) throws Exception {
		return mockMvc.perform(delete(ME).header(MemberIdHeader.NAME, id));
	}
}
