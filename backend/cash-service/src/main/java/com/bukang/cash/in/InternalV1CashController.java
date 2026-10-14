package com.bukang.cash.in;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bukang.cash.app.CashFacade;
import com.bukang.cash.dto.CashPaymentDto;
import com.bukang.cash.dto.CashPaymentRequestDto;
import com.bukang.common.global.rsdata.RsData;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 서비스끼리 부르는 내부 API. 게이트웨이에 라우팅하지 않는다(/internal/** 경로)
 * 회원이 아니라 서비스가 부르므로 회원 ID 헤더를 받지 않는다 (file-service, order-service와 같은 방식)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/cash")
public class InternalV1CashController {
	private final CashFacade cashFacade;

	@PostMapping("/orders/{orderId}/payments")
	public ResponseEntity<RsData<CashPaymentDto>> pay(
		@PathVariable int orderId,
		@Valid @RequestBody CashPaymentRequestDto request
	) {
		CashPaymentDto result = cashFacade.completeOrderPayment(orderId, request.getMemberId(), request.getAmount());
		return ResponseEntity.ok().body(RsData.of(HttpStatus.OK, result.message(), result));
	}
}
