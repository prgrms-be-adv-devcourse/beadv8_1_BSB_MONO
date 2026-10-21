package com.bukang.member.in;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.bukang.member.app.MemberFacade;
import com.bukang.member.domain.Member;
import com.bukang.member.domain.TermsType;
import com.bukang.member.dto.MemberJoinRequestDto;
import com.bukang.member.out.MemberRepository;
import com.bukang.member.out.TermsAgreementRepository;

// 정산 서비스가 관리자 API를 받을 때 부르는 관리자 여부 조회 (PRO-265)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternalV1MemberTest {
	private static final String ADMIN_PATH = "/internal/v1/member/members/{memberId}/admin";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private MemberFacade memberFacade;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private TermsAgreementRepository termsAgreementRepository;

	@AfterEach
	void tearDown() {
		// 동의 기록이 회원을 참조하므로(FK) 먼저 지운다
		termsAgreementRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("관리자면 admin=true를 돌려준다")
	void admin() throws Exception {
		int adminId = join("admin@crewrun.com", "관리자", "010-1234-5670");
		memberFacade.grantAdmin("admin@crewrun.com");

		mockMvc.perform(get(ADMIN_PATH, adminId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("관리자 여부를 조회했습니다."))
			.andExpect(jsonPath("$.data.admin").value(true));
	}

	@Test
	@DisplayName("일반 회원이면 403이 아니라 200 admin=false를 돌려준다")
	void notAdmin() throws Exception {
		int memberId = join("runner1@crewrun.com", "러너1", "010-1234-5671");

		mockMvc.perform(get(ADMIN_PATH, memberId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.admin").value(false));
	}

	@Test
	@DisplayName("탈퇴한 관리자는 없는 회원으로 보고 404 MEMBER_NOT_FOUND를 돌려준다")
	void withdrawnAdminIsNotFound() throws Exception {
		int adminId = join("admin@crewrun.com", "관리자", "010-1234-5670");
		memberFacade.grantAdmin("admin@crewrun.com");
		Member admin = memberRepository.findById(adminId).orElseThrow();
		admin.withdraw(LocalDateTime.now());
		memberRepository.saveAndFlush(admin);

		mockMvc.perform(get(ADMIN_PATH, adminId))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
	}

	@Test
	@DisplayName("없는 회원이면 404 MEMBER_NOT_FOUND를 돌려준다")
	void unknownMemberIsNotFound() throws Exception {
		mockMvc.perform(get(ADMIN_PATH, 999_999))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
	}

	private int join(String email, String nickname, String phone) {
		return memberFacade.join(new MemberJoinRequestDto(
			email, "password1234", "김러너", nickname, phone, LocalDate.of(1995, 4, 12), null,
			List.of(TermsType.SERVICE, TermsType.PRIVACY)
		)).getId();
	}
}
