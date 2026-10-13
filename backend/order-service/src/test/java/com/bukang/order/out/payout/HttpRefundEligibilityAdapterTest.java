package com.bukang.order.out.payout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.bukang.order.domain.exception.ExternalServiceUnavailableException;

class HttpRefundEligibilityAdapterTest {
	private static final String BASE_URL = "http://payout";
	private static final LocalDate RACE_DATE = LocalDate.of(2026, 11, 15);
	private static final String URL = BASE_URL + "/internal/v1/payout/refund-eligibility?raceDate=2026-11-15";
	private static final String NULL_DATA = "{ \"status\": 200, \"message\": \"조회\", \"data\": null }";
	private static final String EMPTY_DATA = "{ \"status\": 200, \"message\": \"조회\", \"data\": {} }";

	private MockRestServiceServer server;
	private HttpRefundEligibilityAdapter adapter;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		adapter = new HttpRefundEligibilityAdapter(builder.build());
	}

	@Test
	@DisplayName("정산 서비스에 대회 날짜로 묻고, 응답의 refundable 값을 돌려준다")
	void refundable() {
		server.expect(requestTo(URL)).andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess(response(true), MediaType.APPLICATION_JSON));
		server.expect(requestTo(URL))
			.andRespond(withSuccess(response(false), MediaType.APPLICATION_JSON));

		assertThat(adapter.isRefundable(RACE_DATE)).isTrue();
		assertThat(adapter.isRefundable(RACE_DATE)).isFalse();
		server.verify();
	}

	@Test
	@DisplayName("정산 서비스가 5xx로 답하거나 응답이 늦으면 503 예외로 알린다")
	void unavailable() {
		server.expect(requestTo(URL)).andRespond(withServerError());
		server.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("read timed out")));

		assertThatThrownBy(() -> adapter.isRefundable(RACE_DATE))
			.isInstanceOf(ExternalServiceUnavailableException.class)
			.hasMessage(HttpRefundEligibilityAdapter.UNAVAILABLE_MESSAGE);
		assertThatThrownBy(() -> adapter.isRefundable(RACE_DATE))
			.isInstanceOf(ExternalServiceUnavailableException.class);
	}

	@Test
	@DisplayName("응답에 refundable 값이 없으면 환불해도 되는지 알 수 없으므로 503 예외로 알린다")
	void missingValue() {
		server.expect(requestTo(URL)).andRespond(withSuccess(NULL_DATA, MediaType.APPLICATION_JSON));
		server.expect(requestTo(URL)).andRespond(withSuccess(EMPTY_DATA, MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> adapter.isRefundable(RACE_DATE))
			.isInstanceOf(ExternalServiceUnavailableException.class);
		assertThatThrownBy(() -> adapter.isRefundable(RACE_DATE))
			.isInstanceOf(ExternalServiceUnavailableException.class);
	}

	// 정산 서비스 InternalV1PayoutRefundController가 실제로 돌려주는 모양
	private String response(boolean refundable) {
		return """
			{ "status": 200, "message": "환불 가능 여부를 조회했습니다.",
				"data": { "raceDate": "2026-11-15", "payoutTargetDate": "2026-11-16", "refundable": %s } }
			""".formatted(refundable);
	}
}
