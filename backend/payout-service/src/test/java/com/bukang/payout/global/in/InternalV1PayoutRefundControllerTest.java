package com.bukang.payout.global.in;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.bukang.payout.out.RacePort;
import com.bukang.payout.out.RaceSchedule;
import com.bukang.payout.support.RestDocsConfig;
import com.bukang.payout.support.StubRacePort;

// 컨트롤러가 오늘 날짜(한국)로 판단하므로, 대회일을 오늘 기준 상대 날짜로 만든다
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Import(RestDocsConfig.class)
@ActiveProfiles("test")
class InternalV1PayoutRefundControllerTest {
	private static final String URL = "/internal/v1/payout/refund-eligibility";
	private static final int KNOWN_TODAY_DEADLINE = 70;
	private static final int KNOWN_PAST_DEADLINE = 71;
	private static final int UNKNOWN_RACE = 99;

	private static final Snippet ELIGIBILITY_RESPONSE = responseFields(
		fieldWithPath("status").description("HTTP 상태 코드"),
		fieldWithPath("message").description("결과 메시지"),
		fieldWithPath("data.raceDate").description("물어본 대회일"),
		fieldWithPath("data.refundDeadline").description(
			"대회의 환불 마감일. 상품 서비스에서 찾지 못하면 null").optional().type(JsonFieldType.STRING),
		fieldWithPath("data.payoutTargetDate").description(
			"이날부터 환불할 수 없다. 환불 마감일을 알면 1차 정산 대상일(마감일 다음 날), 모르면 대회 다음 날"),
		fieldWithPath("data.refundable").description("환불 가능 여부 (오늘이 위 날짜 전이면 true)")
	);

	private static final Snippet ERROR_RESPONSE = responseFields(
		fieldWithPath("status").description("HTTP 상태 코드"),
		fieldWithPath("message").description("실패 이유"),
		fieldWithPath("data").description("실패 응답에서는 항상 null").type(JsonFieldType.NULL)
	);

	@Autowired
	private MockMvc mockMvc;

	private static LocalDate today() {
		return LocalDate.now(ZoneId.of("Asia/Seoul"));
	}

	private ResultActions ask(String raceDate) throws Exception {
		return mockMvc.perform(get(URL).param("raceDate", raceDate));
	}

	private ResultActions ask(int raceId, String raceDate) throws Exception {
		return mockMvc.perform(get(URL).param("raceId", String.valueOf(raceId)).param("raceDate", raceDate));
	}

	// 상품 서비스에 환불 마감일이 있는 대회: 70은 오늘 마감, 71은 어제 마감. 대회일은 둘 다 열흘 뒤다
	@TestConfiguration
	static class RaceScheduleConfig {
		@Bean
		@Primary
		RacePort racePort() {
			LocalDate raceDate = today().plusDays(10);
			return new StubRacePort(raceId -> Optional.of("대회 " + raceId), List.of(
				new RaceSchedule(KNOWN_TODAY_DEADLINE, raceDate, today()),
				new RaceSchedule(KNOWN_PAST_DEADLINE, raceDate, today().minusDays(1))));
		}
	}

	@Test
	@DisplayName("환불 마감일이 오늘이면 환불할 수 있다 (마감일 23:59:59까지). 내일이 1차 정산 대상일이다")
	void refundableOnRefundDeadline() throws Exception {
		LocalDate raceDate = today().plusDays(10);

		ask(KNOWN_TODAY_DEADLINE, raceDate.toString())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.refundDeadline").value(today().toString()))
			.andExpect(jsonPath("$.data.payoutTargetDate").value(today().plusDays(1).toString()))
			.andExpect(jsonPath("$.data.refundable").value(true))
			.andDo(document("payout-refund-eligibility-by-deadline",
				queryParameters(
					parameterWithName("raceId").description("대회 ID. 주면 환불 마감일로 판단한다").optional(),
					parameterWithName("raceDate").description("참가권의 대회일 (yyyy-MM-dd)")),
				ELIGIBILITY_RESPONSE));
	}

	@Test
	@DisplayName("환불 마감일이 어제면 대회 전이어도 환불할 수 없다 (1차 정산이 생긴 날부터)")
	void notRefundableAfterRefundDeadline() throws Exception {
		ask(KNOWN_PAST_DEADLINE, today().plusDays(10).toString())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.payoutTargetDate").value(today().toString()))
			.andExpect(jsonPath("$.data.refundable").value(false));
	}

	@Test
	@DisplayName("상품 서비스에서 환불 마감일을 찾지 못하면 대회일 기준(대회 다음 날부터 불가)으로 판단한다")
	void fallsBackToRaceDateWhenDeadlineUnknown() throws Exception {
		LocalDate raceDate = today();

		ask(UNKNOWN_RACE, raceDate.toString())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.refundDeadline").doesNotExist())
			.andExpect(jsonPath("$.data.payoutTargetDate").value(raceDate.plusDays(1).toString()))
			.andExpect(jsonPath("$.data.refundable").value(true));
	}

	@Test
	@DisplayName("대회 당일이면 정산 대상일(내일) 전이라 환불할 수 있다")
	void refundableOnRaceDay() throws Exception {
		LocalDate raceDate = today();

		ask(raceDate.toString())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("환불 가능 여부를 조회했습니다."))
			.andExpect(jsonPath("$.data.raceDate").value(raceDate.toString()))
			.andExpect(jsonPath("$.data.payoutTargetDate").value(raceDate.plusDays(1).toString()))
			.andExpect(jsonPath("$.data.refundable").value(true))
			.andDo(document("payout-refund-eligibility",
				queryParameters(parameterWithName("raceDate").description("참가권의 대회일 (yyyy-MM-dd)")),
				ELIGIBILITY_RESPONSE));
	}

	@Test
	@DisplayName("어제 열린 대회면 오늘이 정산 대상일이라 환불할 수 없다")
	void notRefundableOnTargetDate() throws Exception {
		LocalDate raceDate = today().minusDays(1);

		ask(raceDate.toString())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.payoutTargetDate").value(today().toString()))
			.andExpect(jsonPath("$.data.refundable").value(false))
			.andDo(document("payout-refund-eligibility-not-refundable", ELIGIBILITY_RESPONSE));
	}

	@Test
	@DisplayName("대회일이 없으면 400")
	void missingRaceDate() throws Exception {
		mockMvc.perform(get(URL))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("필수 요청 값이 없습니다."))
			.andDo(document("payout-refund-eligibility-missing", ERROR_RESPONSE));
	}

	@Test
	@DisplayName("대회일이 날짜 형식이 아니면 400")
	void invalidRaceDate() throws Exception {
		ask("2026/09/25")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("요청 값의 형식이 올바르지 않습니다."))
			.andDo(document("payout-refund-eligibility-bad-request", ERROR_RESPONSE));
	}
}
