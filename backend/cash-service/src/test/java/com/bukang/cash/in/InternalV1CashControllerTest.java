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
import com.bukang.cash.domain.payment.PaymentStatus;
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
		mockMvc.perform(post("/internal/v1/cash/payments/" + ORDER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("결제 완료된 주문을 취소(주문 서비스 요청)하면 고객 잔액이 돌아오고 보관 잔액은 빠진다")
	void cancelCompletedPayment() throws Exception {
		pay(ORDER_ID, CUSTOMER_ID, 3_000).andExpect(status().isOk());

		cancelPayment(ORDER_ID, CUSTOMER_ID).andExpect(status().isOk());

		assertBalance(CUSTOMER_ID, 10_000);
		assertBalance(cashWalletProperties.holdingMemberId(), 0);
		assertThat(countLedgersForOrder(ORDER_ID)).as("결제 2줄 + 취소 반대 2줄").isEqualTo(4);
		assertThat(paymentRepository.findByOrderId(ORDER_ID).orElseThrow().getStatus())
			.isEqualTo(PaymentStatus.CANCELED);
	}

	@Test
	@DisplayName("같은 주문을 두 번 취소해도 잔액은 한 번만 바뀐다")
	void cancelPaymentIsIdempotent() throws Exception {
		pay(ORDER_ID, CUSTOMER_ID, 3_000).andExpect(status().isOk());

		cancelPayment(ORDER_ID, CUSTOMER_ID).andExpect(status().isOk());
		cancelPayment(ORDER_ID, CUSTOMER_ID).andExpect(status().isOk());

		assertBalance(CUSTOMER_ID, 10_000);
	}

	@Test
	@DisplayName("결제 전 주문을 취소로 선점하면, 뒤늦게 온 결제 요청은 거절된다")
	void cancelBeforePaymentRejectsLatePay() throws Exception {
		// 첫 번째 pay 요청이 네트워크 지연 등으로 늦게 도착하는 사이, 주문 서비스가 먼저 이 주문을 포기하고
		// cancelPayment를 보낼 수 있다. 이때 cash에는 아직 이 주문의 결제 기록이 없다.
		// 여기서 아무것도 안 하고 끝내면, 뒤늦게 도착한 pay가 정상 결제로 처리돼 버려
		// 이미 포기된 주문인데 고객 돈만 빠지는 상태가 된다.
		// 그래서 결제 기록이 없어도 CANCELED로 미리 선점해 두고, 나중에 온 pay를 거절한다.
		cancelPayment(ORDER_ID, CUSTOMER_ID).andExpect(status().isOk());

		pay(ORDER_ID, CUSTOMER_ID, 3_000)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("이미 취소·환불된 결제입니다."));

		assertBalance(CUSTOMER_ID, 10_000);
	}

	private ResultActions pay(int orderId, int memberId, int amount) throws Exception {
		return mockMvc.perform(post("/internal/v1/cash/payments/" + orderId)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"memberId\": %d, \"amount\": %d}".formatted(memberId, amount)));
	}

	private ResultActions cancelPayment(int orderId, int memberId) throws Exception {
		return mockMvc.perform(post("/internal/v1/cash/payments/" + orderId + "/cancel")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"memberId\": %d}".formatted(memberId)));
	}

	private void assertBalance(int holderId, int expected) {
		assertThat(walletRepository.findById(holderId).orElseThrow().getBalance()).isEqualTo(expected);
	}

	private long countLedgersForOrder(int orderId) {
		return walletLedgerRepository.findAll().stream()
			.filter(ledger -> ledger.getRefTypeCode() == LedgerRefType.Order && ledger.getRefId() == orderId)
			.count();
	}

	private Wallet createWallet(int memberId, String nickname) {
		LocalDateTime now = LocalDateTime.of(2026, 10, 12, 10, 0);
		CashMember member = cashMemberRepository.save(new CashMember(memberId, now, now, nickname));
		return walletRepository.save(new Wallet(member));
	}
}
