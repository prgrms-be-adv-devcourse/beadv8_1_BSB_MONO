package com.bukang.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 판매자 키트 배송 관리 화면 위쪽의 건수 (시안 S08, PRO-136). 결제한 참가권의 키트만 센다
 * 환불 기한 전 건수는 배송 준비 중 아직 환불 기한이 지나지 않아 발송할 수 없는 것이다 (배송 준비 건수에 들어 있음)
 */
@AllArgsConstructor
@Getter
public class SellerKitSummaryDto {
	private final long preparing;
	private final long shipping;
	private final long delivered;
	private final long beforeRefundDeadline;
}
