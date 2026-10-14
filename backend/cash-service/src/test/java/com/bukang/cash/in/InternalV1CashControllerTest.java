package com.bukang.cash.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import com.bukang.cash.config.CashWalletProperties;
import com.bukang.cash.domain.member.CashMember;
import com.bukang.cash.domain.wallet.LedgerRefType;
import com.bukang.cash.domain.wallet.LedgerType;
import com.bukang.cash.domain.wallet.Wallet;
import com.bukang.cash.out.CashMemberRepository;
import com.bukang.cash.out.PaymentRepository;
import com.bukang.cash.out.WalletLedgerRepository;
import com.bukang.cash.out.WalletRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternalV1CashControllerTest {
	private static final int CUSTOMER_ID = 3;
	private static final int ORDER_ID = 100;

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private CashWalletProperties cashWalletProperties;
	@Autowired
	private CashMemberRepository cashMemberRepository;
	@Autowired
	private WalletRepository walletRepository;
	@Autowired
	private WalletLedgerRepository walletLedgerRepository;
	@Autowired
	private PaymentRepository paymentRepository;
	@Autowired
	private TransactionTemplate transactionTemplate;

	@BeforeEach
	void setUp() {
		transactionTemplate.executeWithoutResult(status -> {
			createWallet(cashWalletProperties.holdingMemberId(), "홀딩");
			Wallet customerWallet = createWallet(CUSTOMER_ID, "러너3");
			walletLedgerRepository.save(
				customerWallet.credit(10_000, LedgerType.충전__PG결제_토스페이먼츠, LedgerRefType.WalletCharge, 1));
		});
	}

	@AfterEach
	void tearDown() {
		paymentRepository.deleteAll();
		walletLedgerRepository.deleteAll();
		walletRepository.deleteAll();
		cashMemberRepository.deleteAll();
	}

	@Test
	@DisplayName("잔액이 충분하면 결제가 되고 고객 지갑은 줄고 홀딩 지갑은 늘어난다")
	void payWithSufficientBalance() throws Exception {
		pay(ORDER_ID, CUSTOMER_ID, 3_000)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("결제했습니다."))
			.andExpect(jsonPath("$.data.outcome").value("PAID"))
			.andExpect(jsonPath("$.data.shortage").value(0))
			.andExpect(jsonPath("$.data.message").doesNotExist());

		assertBalance(CUSTOMER_ID, 7_000);
		assertBalance(cashWalletProperties.holdingMemberId(), 3_000);
	}

	@Test
	@DisplayName("잔액이 부족하면 차감 없이 부족한 금액을 돌려준다")
	void payWithInsufficientBalance() throws Exception {
		pay(ORDER_ID, CUSTOMER_ID, 15_000)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("예치금이 부족합니다."))
			.andExpect(jsonPath("$.data.outcome").value("INSUFFICIENT_BALANCE"))
			.andExpect(jsonPath("$.data.shortage").value(5_000));

		assertBalance(CUSTOMER_ID, 10_000);
	}

	@Test
	@DisplayName("같은 주문 ID로 다시 결제 요청해도 중복 차감 없이 PAID만 돌아온다")
	void payIsIdempotentByOrderId() throws Exception {
		pay(ORDER_ID, CUSTOMER_ID, 3_000).andExpect(status().isOk());

		pay(ORDER_ID, CUSTOMER_ID, 3_000)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.outcome").value("PAID"));

		assertBalance(CUSTOMER_ID, 7_000);
	}

	@Test
	@DisplayName("존재하지 않는 회원의 지갑으로 결제하면 404다")
	void payWithoutWallet() throws Exception {
		pay(ORDER_ID, 999, 1_000)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("존재하지 않는 지갑입니다."));
	}

	@Test
	@DisplayName("회원 ID나 금액이 없으면 400이다")
	void invalidRequest() throws Exception {
		mockMvc.perform(post("/internal/v1/cash/orders/" + ORDER_ID + "/payments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest());
	}

	private ResultActions pay(int orderId, int memberId, int amount) throws Exception {
		return mockMvc.perform(post("/internal/v1/cash/orders/" + orderId + "/payments")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"memberId\": %d, \"amount\": %d}".formatted(memberId, amount)));
	}

	private void assertBalance(int holderId, int expected) {
		assertThat(walletRepository.findById(holderId).orElseThrow().getBalance()).isEqualTo(expected);
	}

	private Wallet createWallet(int memberId, String nickname) {
		LocalDateTime now = LocalDateTime.of(2026, 10, 12, 10, 0);
		CashMember member = cashMemberRepository.save(new CashMember(memberId, now, now, nickname));
		return walletRepository.save(new Wallet(member));
	}
}
