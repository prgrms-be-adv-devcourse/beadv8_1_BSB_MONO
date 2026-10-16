package com.bukang.member.out.cash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.bukang.member.domain.exception.WithdrawCheckFailedException;

class HttpCashBalanceAdapterTest {
	private static final String BASE_URL = "http://cash";
	private static final String URL = BASE_URL + "/internal/v1/cash/wallets/3/balance";

	private MockRestServiceServer server;
	private HttpCashBalanceAdapter adapter;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		adapter = new HttpCashBalanceAdapter(builder.build());
	}

	@Test
	@DisplayName("예치금 서비스에 회원 ID로 묻고, 응답의 balance 값을 돌려준다")
	void balance() {
		server.expect(requestTo(URL)).andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("""
				{ "status": 200, "message": "잔액을 조회했습니다.", "data": { "balance": 1500 } }
				""", MediaType.APPLICATION_JSON));

		assertThat(adapter.findBalance(3)).isEqualTo(1500);
		server.verify();
	}

	@Test
	@DisplayName("5xx·4xx·시간 초과·값 없는 응답은 모두 잔액을 모르는 것이라 503 예외로 알린다")
	void unavailable() {
		server.expect(requestTo(URL)).andRespond(withServerError());
		server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));
		server.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("read timed out")));
		server.expect(requestTo(URL)).andRespond(withSuccess("""
			{ "status": 200, "message": "잔액을 조회했습니다.", "data": null }
			""", MediaType.APPLICATION_JSON));

		for (int attempt = 0; attempt < 4; attempt++) {
			assertThatThrownBy(() -> adapter.findBalance(3))
				.isInstanceOf(WithdrawCheckFailedException.class)
				.hasMessage(HttpCashBalanceAdapter.UNAVAILABLE_MESSAGE);
		}
		server.verify();
	}
}
