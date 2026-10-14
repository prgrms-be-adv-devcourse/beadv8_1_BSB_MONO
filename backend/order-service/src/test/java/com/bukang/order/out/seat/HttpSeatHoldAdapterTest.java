package com.bukang.order.out.seat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.bukang.common.global.json.JsonConverter;
import com.bukang.common.shared.kafka.KafkaTopics;
import com.bukang.order.domain.exception.ExternalServiceUnavailableException;
import com.bukang.order.domain.exception.OrderNotExtendableException;
import com.bukang.order.domain.exception.ParticipationNotPayableException;
import com.bukang.order.domain.exception.RaceCourseNotFoundException;

import tools.jackson.databind.json.JsonMapper;

class HttpSeatHoldAdapterTest {
	private static final String BASE_URL = "http://product";
	private static final String HOLD_URL = BASE_URL + "/internal/v1/product/holds";
	private static final String SELL_URL = HOLD_URL + "/1001/sell";
	private static final String RESTORE_URL = HOLD_URL + "/1001/restore";
	private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 10, 14, 19, 10);

	private MockRestServiceServer server;
	private HttpSeatHoldAdapter adapter;
	private KafkaTemplate<String, String> kafkaTemplate;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		kafkaTemplate = mock();
		JsonConverter jsonConverter = new JsonConverter(JsonMapper.builder().build());
		adapter = new HttpSeatHoldAdapter(builder.build(), kafkaTemplate, jsonConverter);
	}

	@Test
	@DisplayName("확보: 종목·참가 신청 ID와 주문이 정한 결제 기한을 보내고, 확보면 기한을, 매진이면 매진을 돌려준다")
	void hold() {
		server.expect(requestTo(HOLD_URL)).andExpect(method(HttpMethod.POST))
			.andExpect(content().json(
				"{ \"raceCourseId\": 7, \"participationId\": 1001, \"expiresAt\": \"2026-10-14T19:10:00\" }"))
			.andRespond(withSuccess(data("{ \"held\": true, \"expiresAt\": \"2026-10-14T19:10:00\" }"),
				MediaType.APPLICATION_JSON));
		server.expect(requestTo(HOLD_URL))
			.andRespond(withSuccess(data("{ \"held\": false, \"expiresAt\": null }"), MediaType.APPLICATION_JSON));

		assertThat(adapter.hold(7, 1001, EXPIRES_AT)).isEqualTo(SeatHoldResult.held(EXPIRES_AT));
		assertThat(adapter.hold(7, 1001, EXPIRES_AT)).isEqualTo(SeatHoldResult.soldOut());
		server.verify();
	}

	@Test
	@DisplayName("확보: 없는 종목(404)은 종목 없음, 409(접수 기간 아님·이미 끝난 신청)는 상품의 이유 그대로 409로 알린다")
	void holdRejected() {
		server.expect(requestTo(HOLD_URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));
		server.expect(requestTo(HOLD_URL)).andRespond(withStatus(HttpStatus.CONFLICT)
			.contentType(MediaType.APPLICATION_JSON)
			.body("{ \"status\": 409, \"message\": \"접수 기간이 아닙니다.\", \"data\": null }"));
		server.expect(requestTo(HOLD_URL)).andRespond(withStatus(HttpStatus.CONFLICT));

		assertThatThrownBy(() -> adapter.hold(7, 1001, EXPIRES_AT)).isInstanceOf(RaceCourseNotFoundException.class);
		assertThatThrownBy(() -> adapter.hold(7, 1001, EXPIRES_AT))
			.isInstanceOf(ParticipationNotPayableException.class)
			.hasMessage("접수 기간이 아닙니다.");
		assertThatThrownBy(() -> adapter.hold(7, 1001, EXPIRES_AT))
			.isInstanceOf(ParticipationNotPayableException.class)
			.hasMessage(HttpSeatHoldAdapter.HOLD_REJECTED_MESSAGE);
		server.verify();
	}

	@Test
	@DisplayName("확보: 5xx·응답 지연·형식이 다른 응답(확보인데 기한 없음)은 503으로 알린다 (같은 신청으로 다시 하면 된다)")
	void holdUnavailable() {
		server.expect(requestTo(HOLD_URL)).andRespond(withServerError());
		server.expect(requestTo(HOLD_URL)).andRespond(withException(new SocketTimeoutException("read timed out")));
		server.expect(requestTo(HOLD_URL))
			.andRespond(withSuccess(data("{ \"held\": true }"), MediaType.APPLICATION_JSON));
		server.expect(requestTo(HOLD_URL)).andRespond(withSuccess(data("null"), MediaType.APPLICATION_JSON));
		// 400(요청한 끝 시각이 상품 시계로 이미 지남 등)은 계약 어긋남으로 남긴다
		server.expect(requestTo(HOLD_URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

		for (int attempt = 0; attempt < 5; attempt++) {
			assertThatThrownBy(() -> adapter.hold(7, 1001, EXPIRES_AT))
				.isInstanceOf(ExternalServiceUnavailableException.class)
				.hasMessage(HttpSeatHoldAdapter.UNAVAILABLE_MESSAGE);
		}
		server.verify();
	}

	@Test
	@DisplayName("판매: 참가 신청 ID로 요청하고 SOLD·EXPIRED를 그대로 옮긴다. 결과가 없거나 실패하면 503")
	void sell() {
		server.expect(requestTo(SELL_URL)).andExpect(method(HttpMethod.POST))
			.andRespond(withSuccess(data("{ \"result\": \"SOLD\" }"), MediaType.APPLICATION_JSON));
		server.expect(requestTo(SELL_URL))
			.andRespond(withSuccess(data("{ \"result\": \"EXPIRED\" }"), MediaType.APPLICATION_JSON));
		server.expect(requestTo(SELL_URL)).andRespond(withSuccess(data("{}"), MediaType.APPLICATION_JSON));
		server.expect(requestTo(SELL_URL)).andRespond(withServerError());

		assertThat(adapter.sell(1001)).isEqualTo(SeatSellResult.SOLD);
		assertThat(adapter.sell(1001)).isEqualTo(SeatSellResult.EXPIRED);
		assertThatThrownBy(() -> adapter.sell(1001)).isInstanceOf(ExternalServiceUnavailableException.class);
		assertThatThrownBy(() -> adapter.sell(1001)).isInstanceOf(ExternalServiceUnavailableException.class);
		server.verify();
	}

	@Test
	@DisplayName("복구: 참가 신청 ID로 요청한다. 되돌릴 자리가 없어도(restored=false) 성공이고, 실패하면 503")
	void restore() {
		server.expect(requestTo(RESTORE_URL)).andExpect(method(HttpMethod.POST))
			.andRespond(withSuccess(data("{ \"restored\": true }"), MediaType.APPLICATION_JSON));
		server.expect(requestTo(RESTORE_URL))
			.andRespond(withSuccess(data("{ \"restored\": false }"), MediaType.APPLICATION_JSON));
		server.expect(requestTo(RESTORE_URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));

		adapter.restore(1001);
		adapter.restore(1001);
		assertThatThrownBy(() -> adapter.restore(1001)).isInstanceOf(ExternalServiceUnavailableException.class);
		server.verify();
	}

	@Test
	@DisplayName("연장: 상품에 연장 API가 없어 부르지 않고 늘릴 수 없다고(409) 알린다")
	void extend() {
		assertThatThrownBy(() -> adapter.extend(1001))
			.isInstanceOf(OrderNotExtendableException.class)
			.hasMessage(HttpSeatHoldAdapter.NOT_EXTENDABLE_MESSAGE);
		server.verify();
	}

	@Test
	@DisplayName("해제: 참가 신청 ID를 key로 product.hold.cancel에 {participationId}를 보내고, 보내지 못하면 503으로 알린다")
	void release() {
		given(kafkaTemplate.send(KafkaTopics.PRODUCT_HOLD_CANCEL, "1001", "{\"participationId\":1001}"))
			.willReturn(CompletableFuture.completedFuture(null));

		adapter.release(1001);

		verify(kafkaTemplate).send(KafkaTopics.PRODUCT_HOLD_CANCEL, "1001", "{\"participationId\":1001}");

		// 브로커에 연결하지 못하면 send()가 예외를 던지고, 브로커가 받지 못하면 future가 실패한다
		// 둘 다 503으로 알려 만료를 롤백시키고 다음 실행에서 다시 보내게 한다
		given(kafkaTemplate.send(KafkaTopics.PRODUCT_HOLD_CANCEL, "1002", "{\"participationId\":1002}"))
			.willThrow(new KafkaException("broker unavailable"));
		given(kafkaTemplate.send(KafkaTopics.PRODUCT_HOLD_CANCEL, "1003", "{\"participationId\":1003}"))
			.willReturn(CompletableFuture.failedFuture(new KafkaException("send failed")));
		assertThatThrownBy(() -> adapter.release(1002)).isInstanceOf(ExternalServiceUnavailableException.class);
		assertThatThrownBy(() -> adapter.release(1003)).isInstanceOf(ExternalServiceUnavailableException.class);
		server.verify();
	}

	private String data(String body) {
		return "{ \"status\": 200, \"message\": \"처리\", \"data\": " + body + " }";
	}
}
