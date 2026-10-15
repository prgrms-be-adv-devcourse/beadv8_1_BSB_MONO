package com.bukang.cash.out.pg;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.bukang.cash.domain.exception.PaymentApprovalFailedException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Toss페이먼츠 결제 승인 API를 부른다 (POST /v1/payments/confirm, PRO-81)
 * 시크릿 키를 Basic 인증으로 보낸다. 등록은 PaymentClientConfig가 한다 (cash.clients.pg.mode=http)
 */
@Slf4j
@RequiredArgsConstructor
public class TossPaymentClient implements PaymentClient {
	static final String CONFIRM_PATH = "/v1/payments/confirm";

	private final RestClient restClient;
	private final String secretKey;

	@Override
	public PaymentApprovalResult approve(String paymentKey, String orderId, int amount) {
		ConfirmResponse response;
		try {
			response = restClient.post()
				.uri(CONFIRM_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.headers(headers -> headers.setBasicAuth(secretKey, ""))
				.body(new ConfirmRequest(paymentKey, orderId, amount))
				.retrieve()
				.body(ConfirmResponse.class);
		} catch (RestClientResponseException exception) {
			// Toss는 거절 시 {code, message} 형태로 응답한다
			log.error("Toss 결제 승인이 거절됐습니다. paymentKey={}, orderId={}, status={}, body={}",
				paymentKey, orderId, exception.getStatusCode(), exception.getResponseBodyAsString());
			throw new PaymentApprovalFailedException("PG사가 결제 승인을 거절했습니다.");
		} catch (RestClientException exception) {
			log.error("Toss 결제 승인 서버에 연결하지 못했습니다. paymentKey={}, orderId={}", paymentKey, orderId, exception);
			throw new PaymentApprovalFailedException("PG 서버에 연결하지 못했습니다. 잠시 후 다시 시도해주세요.");
		}
		if (response == null) {
			log.error("Toss 결제 승인 응답이 비어 있습니다. paymentKey={}, orderId={}", paymentKey, orderId);
			throw new PaymentApprovalFailedException("PG 서버 응답이 올바르지 않습니다.");
		}
		return new PaymentApprovalResult(response.totalAmount(), response.paymentKey());
	}

	private record ConfirmRequest(String paymentKey, String orderId, int amount) {
	}

	// Toss 응답(Payment 객체)엔 필드가 훨씬 많지만, 지갑에 반영할 때 쓰는 값만 받는다
	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ConfirmResponse(String paymentKey, int totalAmount) {
	}
}
