package com.bukang.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 판매자 키트 배송 관리 화면 위쪽의 건수 (시안 S08, PRO-136). 목록과 같은 기준(결제했거나 이미 보낸 키트)으로 센다
 * 환불 기한 전 건수는 배송 준비 중 아직 환불할 수 있어 발송할 수 없는 것이다 (배송 준비 건수에 들어 있음)
 */
@AllArgsConstructor
@Getter
public class SellerKitSummaryDto {
	private final long preparing;
	private final long shipping;
	private final long delivered;
	private final long beforeRefundDeadline;
}
