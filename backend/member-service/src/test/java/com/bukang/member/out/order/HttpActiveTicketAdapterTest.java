package com.bukang.member.out.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.bukang.member.domain.exception.WithdrawCheckFailedException;

class HttpActiveTicketAdapterTest {
	private static final String BASE_URL = "http://order";
	private static final String URL = BASE_URL + "/internal/v1/order/members/3/active-tickets/count";

	private MockRestServiceServer server;
	private HttpActiveTicketAdapter adapter;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		adapter = new HttpActiveTicketAdapter(builder.build());
	}

	@Test
	@DisplayName("주문 서비스에 회원 ID로 묻고, 응답의 count 값을 돌려준다")
	void count() {
		server.expect(requestTo(URL)).andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("""
				{ "status": 200, "message": "끝나지 않은 참가권 수를 조회했습니다.", "data": { "count": 2 } }
				""", MediaType.APPLICATION_JSON));

		assertThat(adapter.countActiveTickets(3)).isEqualTo(2);
		server.verify();
	}

	@Test
	@DisplayName("5xx·시간 초과·값 없는 응답은 참가권 수를 모르는 것이라 503 예외로 알린다")
	void unavailable() {
		server.expect(requestTo(URL)).andRespond(withServerError());
		server.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("read timed out")));
		server.expect(requestTo(URL)).andRespond(withSuccess("""
			{ "status": 200, "message": "조회", "data": {} }
			""", MediaType.APPLICATION_JSON));

		for (int attempt = 0; attempt < 3; attempt++) {
			assertThatThrownBy(() -> adapter.countActiveTickets(3))
				.isInstanceOf(WithdrawCheckFailedException.class)
				.hasMessage(HttpActiveTicketAdapter.UNAVAILABLE_MESSAGE);
		}
		server.verify();
	}
}
