package com.bukang.cash.out.pg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.bukang.cash.domain.exception.PaymentApprovalFailedException;

class TossPaymentClientTest {
	private static final String BASE_URL = "http://toss";
	private static final String SECRET_KEY = "test_sk_dummy";
	private static final String URL = BASE_URL + TossPaymentClient.CONFIRM_PATH;
	private static final String BASIC_AUTH =
		"Basic " + Base64.getEncoder().encodeToString((SECRET_KEY + ":").getBytes(StandardCharsets.UTF_8));

	private MockRestServiceServer server;
	private TossPaymentClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		client = new TossPaymentClient(builder.build(), SECRET_KEY);
	}

	@Test
	@DisplayName("시크릿 키로 Basic 인증 헤더를 넣고, paymentKey·orderId·amount를 요청 바디에 담아 보낸다")
	void sendsAuthHeaderAndBody() {
		server.expect(requestTo(URL))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header(HttpHeaders.AUTHORIZATION, BASIC_AUTH))
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.paymentKey").value("pay_123"))
			.andExpect(jsonPath("$.orderId").value("order_1"))
			.andExpect(jsonPath("$.amount").value(10_000))
			.andRespond(withSuccess(confirmResponse("pay_123", 10_000), MediaType.APPLICATION_JSON));

		client.approve("pay_123", "order_1", 10_000);

		server.verify();
	}

	@Test
	@DisplayName("승인 성공 시 응답의 승인 금액·결제 키를 돌려준다")
	void approveSuccess() {
		server.expect(requestTo(URL))
			.andRespond(withSuccess(confirmResponse("pay_123", 10_000), MediaType.APPLICATION_JSON));

		PaymentApprovalResult result = client.approve("pay_123", "order_1", 10_000);

		assertThat(result.approvedAmount()).isEqualTo(10_000);
		assertThat(result.pgPaymentKey()).isEqualTo("pay_123");
	}

	@Test
	@DisplayName("응답에 우리가 안 쓰는 필드가 더 있어도 무시하고 파싱한다")
	void ignoresUnknownResponseFields() {
		String responseWithExtraFields = """
			{ "paymentKey": "pay_123", "totalAmount": 10000, "method": "카드", "mId": "tosspayments",
				"approvedAt": "2026-10-15T10:00:00+09:00" }
			""";
		server.expect(requestTo(URL)).andRespond(withSuccess(responseWithExtraFields, MediaType.APPLICATION_JSON));

		PaymentApprovalResult result = client.approve("pay_123", "order_1", 10_000);

		assertThat(result.approvedAmount()).isEqualTo(10_000);
		assertThat(result.pgPaymentKey()).isEqualTo("pay_123");
	}

	@Test
	@DisplayName("Toss가 4xx로 거절하면(예: 존재하지 않는 결제) 결제 승인 실패 예외로 알린다")
	void rejectedWithClientError() {
		server.expect(requestTo(URL))
			.andRespond(withStatus(HttpStatus.NOT_FOUND)
				.body(errorResponse())
				.contentType(MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.approve("pay_123", "order_1", 10_000))
			.isInstanceOf(PaymentApprovalFailedException.class);
	}

	@Test
	@DisplayName("Toss가 5xx로 답하면 결제 승인 실패 예외로 알린다")
	void serverError() {
		server.expect(requestTo(URL)).andRespond(withServerError());

		assertThatThrownBy(() -> client.approve("pay_123", "order_1", 10_000))
			.isInstanceOf(PaymentApprovalFailedException.class);
	}

	@Test
	@DisplayName("Toss 서버에 연결하지 못하면(타임아웃 등) 결제 승인 실패 예외로 알린다")
	void connectionFailure() {
		server.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("read timed out")));

		assertThatThrownBy(() -> client.approve("pay_123", "order_1", 10_000))
			.isInstanceOf(PaymentApprovalFailedException.class);
	}

	@Test
	@DisplayName("응답이 비어 있으면 결제 승인 실패 예외로 알린다")
	void emptyResponseBody() {
		server.expect(requestTo(URL)).andRespond(withSuccess());

		assertThatThrownBy(() -> client.approve("pay_123", "order_1", 10_000))
			.isInstanceOf(PaymentApprovalFailedException.class);
	}

	// Toss 결제 승인 응답의 모양 (우리가 실제로 쓰는 필드만)
	private String confirmResponse(String paymentKey, int totalAmount) {
		return """
			{ "paymentKey": "%s", "totalAmount": %d }
			""".formatted(paymentKey, totalAmount);
	}

	// Toss 오류 응답의 모양
	private String errorResponse() {
		return """
			{ "code": "NOT_FOUND_PAYMENT", "message": "존재하지 않는 결제 정보 입니다." }
			""";
	}
}
