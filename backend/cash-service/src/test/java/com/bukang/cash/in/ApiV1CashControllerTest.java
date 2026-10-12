package com.bukang.cash.in;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.cash.domain.CashMember;
import com.bukang.cash.domain.Wallet;
import com.bukang.cash.out.CashMemberRepository;
import com.bukang.cash.out.WalletRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiV1CashControllerTest {
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
		CashMember member = cashMemberRepository.save(new CashMember(3, now, now, "runner3", "러너3"));
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
}
