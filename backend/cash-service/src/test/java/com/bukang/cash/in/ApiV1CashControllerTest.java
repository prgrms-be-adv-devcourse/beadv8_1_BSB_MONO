package com.bukang.cash.in;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.cash.domain.member.CashMember;
import com.bukang.cash.domain.wallet.Wallet;
import com.bukang.cash.out.CashMemberRepository;
import com.bukang.cash.out.WalletRepository;
import com.bukang.common.shared.member.web.MemberIdHeader;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiV1CashControllerTest {
	// 충전 확정 요청 오류 테스트는 컨트롤러 검증에서 끝나므로 PG(Toss)를 부르지 않는다
	private static final String CHARGE_CONFIRM_BODY = """
		{ "paymentKey": "5EnNZRJGvaBd3GirPNB6s", "orderId": "charge-1697000000000", "amount": 10000 }
		""";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private CashMemberRepository cashMemberRepository;
	@Autowired
	private WalletRepository walletRepository;

	@Test
	@DisplayName("잔액 조회: 지갑이 있으면 200과 RsData 형식으로 잔액을 돌려준다")
	void getBalance() throws Exception {
		LocalDateTime now = LocalDateTime.of(2026, 10, 12, 10, 0);
		CashMember member = cashMemberRepository.save(new CashMember(3, now, now, "러너3"));
		walletRepository.save(new Wallet(member));

		mockMvc.perform(get("/api/v1/cash/wallets/3/balance"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("잔액 조회 성공"))
			.andExpect(jsonPath("$.data.holderId").value(3))
			.andExpect(jsonPath("$.data.balance").value(0));
	}

	@Test
	@DisplayName("잔액 조회: 지갑이 없으면 404와 에러 메시지를 돌려준다")
	void getBalanceWithoutWallet() throws Exception {
		mockMvc.perform(get("/api/v1/cash/wallets/999/balance"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.message").value("존재하지 않는 지갑입니다."))
			.andExpect(jsonPath("$.data").isEmpty());
	}

	@Test
	@DisplayName("충전 확정: 회원 ID 헤더가 없으면 401과 RsData 형식으로 로그인 필요를 알린다")
	void confirmChargeWithoutMemberId() throws Exception {
		mockMvc.perform(post("/api/v1/cash/charges/confirm")
				.contentType(MediaType.APPLICATION_JSON)
				.content(CHARGE_CONFIRM_BODY))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."))
			.andExpect(jsonPath("$.data").isEmpty());
	}

	@Test
	@DisplayName("충전 확정: 회원 ID가 0 이하면 400 INVALID_REQUEST로 응답한다")
	void confirmChargeWithNonPositiveMemberId() throws Exception {
		mockMvc.perform(post("/api/v1/cash/charges/confirm")
				.header(MemberIdHeader.NAME, "0")
				.contentType(MediaType.APPLICATION_JSON)
				.content(CHARGE_CONFIRM_BODY))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value(MemberIdHeader.INVALID_MESSAGE));
	}

	@Test
	@DisplayName("충전 확정: 회원 ID가 숫자가 아니면 400 INVALID_REQUEST로 응답한다")
	void confirmChargeWithNonNumericMemberId() throws Exception {
		mockMvc.perform(post("/api/v1/cash/charges/confirm")
				.header(MemberIdHeader.NAME, "abc")
				.contentType(MediaType.APPLICATION_JSON)
				.content(CHARGE_CONFIRM_BODY))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value("요청 값의 형식이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("충전 확정: 요청 본문 검증에 실패하면 400 INVALID_REQUEST와 첫 오류 메시지로 응답한다")
	void confirmChargeWithInvalidBody() throws Exception {
		mockMvc.perform(post("/api/v1/cash/charges/confirm")
				.header(MemberIdHeader.NAME, "3")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{ "paymentKey": "", "orderId": "charge-1697000000000", "amount": 10000 }
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value("paymentKey를 작성해주세요."));
	}
}
