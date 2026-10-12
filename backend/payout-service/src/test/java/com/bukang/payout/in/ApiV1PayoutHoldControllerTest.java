package com.bukang.payout.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.YearMonth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.bukang.payout.domain.BusinessDayCalendar;
import com.bukang.payout.domain.Payout;
import com.bukang.payout.domain.PayoutStatus;
import com.bukang.payout.out.PayoutHoldRepository;
import com.bukang.payout.out.PayoutRepository;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiV1PayoutHoldControllerTest {
	private static final int ADMIN_ID = 99;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PayoutRepository payoutRepository;

	@Autowired
	private PayoutHoldRepository payoutHoldRepository;

	@AfterEach
	void tearDown() {
		payoutHoldRepository.deleteAll();
		payoutRepository.deleteAll();
	}

	private Payout savePayout() {
		return payoutRepository.save(new Payout(1, 10, YearMonth.of(2026, 9),
			LocalDateTime.of(2026, 9, 29, 3, 0), new BusinessDayCalendar()));
	}

	private ResultActions hold(int payoutId, String body) throws Exception {
		return mockMvc.perform(post("/api/v1/payout/payouts/{payoutId}/holds", payoutId)
			.header("X-Member-Id", ADMIN_ID)
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private ResultActions holdAccount(int payoutId) throws Exception {
		return hold(payoutId, """
			{"reasonType": "ACCOUNT_NOT_REGISTERED", "reason": "계좌 확인 중"}
			""");
	}

	private ResultActions release(int holdId) throws Exception {
		return mockMvc.perform(post("/api/v1/payout/holds/{holdId}/release", holdId)
			.header("X-Member-Id", ADMIN_ID));
	}

	private int holdIdOf(ResultActions result) throws Exception {
		return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.data.id");
	}

	private PayoutStatus statusOf(Payout payout) {
		return payoutRepository.findById(payout.getId()).orElseThrow().getStatus();
	}

	@Test
	@DisplayName("보류를 걸면 201과 지급보류 ID를 돌려주고, 정산 내역은 HELD가 된다")
	void holdPayout() throws Exception {
		Payout payout = savePayout();

		ResultActions result = holdAccount(payout.getId())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value(201))
			.andExpect(jsonPath("$.message").value("지급보류를 걸었습니다."))
			.andExpect(jsonPath("$.data.holdCode").value(startsWith("HD-")))
			.andExpect(jsonPath("$.data.payoutId").value(payout.getId()))
			.andExpect(jsonPath("$.data.reasonType").value("ACCOUNT_NOT_REGISTERED"))
			.andExpect(jsonPath("$.data.status").value("HELD"))
			.andExpect(jsonPath("$.data.heldBy").value(ADMIN_ID));

		result.andExpect(header().string("Location", "/api/v1/payout/holds/" + holdIdOf(result)));
		assertThat(statusOf(payout)).isEqualTo(PayoutStatus.HELD);
	}

	@Test
	@DisplayName("보류를 풀면 200을 돌려주고, 정산 내역은 CALCULATED로 돌아간다")
	void releaseHold() throws Exception {
		Payout payout = savePayout();
		int holdId = holdIdOf(holdAccount(payout.getId()));

		release(holdId)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("지급보류를 풀었습니다."))
			.andExpect(jsonPath("$.data.status").value("RELEASED"))
			.andExpect(jsonPath("$.data.releasedBy").value(ADMIN_ID))
			.andExpect(jsonPath("$.data.releasedAt").isNotEmpty());

		assertThat(statusOf(payout)).isEqualTo(PayoutStatus.CALCULATED);
	}

	@Test
	@DisplayName("이미 보류 중인 정산 내역에 다시 걸면 409")
	void holdTwice() throws Exception {
		Payout payout = savePayout();
		holdAccount(payout.getId());

		holdAccount(payout.getId())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("이미 지급보류 중인 정산 내역입니다."));
	}

	@Test
	@DisplayName("이미 해제된 보류를 다시 풀면 409")
	void releaseTwice() throws Exception {
		Payout payout = savePayout();
		int holdId = holdIdOf(holdAccount(payout.getId()));
		release(holdId);

		release(holdId)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("이미 해제된 지급보류입니다."));
	}

	@Test
	@DisplayName("없는 정산 내역, 없는 보류는 404")
	void notFound() throws Exception {
		holdAccount(9999)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("정산 내역을 찾을 수 없습니다."));
		release(9999)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("지급보류를 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("회원 ID 헤더가 없으면 401")
	void missingMemberId() throws Exception {
		mockMvc.perform(post("/api/v1/payout/holds/{holdId}/release", 1))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("보류 사유가 없거나 없는 사유 값이면 400")
	void invalidReason() throws Exception {
		Payout payout = savePayout();

		hold(payout.getId(), "{}")
			.andExpect(status().isBadRequest());
		hold(payout.getId(), """
			{"reasonType": "UNKNOWN"}
			""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("요청 본문 형식이 올바르지 않습니다."));
		assertThat(statusOf(payout)).isEqualTo(PayoutStatus.CALCULATED);
	}

	@Test
	@DisplayName("경로의 ID가 숫자가 아니면 400")
	void invalidPathId() throws Exception {
		mockMvc.perform(post("/api/v1/payout/holds/{holdId}/release", "abc")
				.header("X-Member-Id", ADMIN_ID))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("요청 값의 형식이 올바르지 않습니다."));
	}
}
