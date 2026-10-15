package com.bukang.order.dto;

import java.time.LocalDateTime;

import com.bukang.order.domain.KitDeliveryStatus;
import com.bukang.order.domain.ShippingAddress;

import lombok.Getter;

/**
 * 판매자 키트 배송 관리 목록의 한 줄 (시안 S08, PRO-136). 결제한 참가권 중 키트가 있는 것만 나온다
 * 구매자 실명·연락처·배송지는 배송에 필요한 범위라 판매자에게 보여 준다 (참가 신청 때 제3자 제공 동의)
 */
@Getter
public class SellerKitDto {
	private final int orderItemId;
	private final String orderNo;
	private final int raceId;
	private final int raceCourseId;
	// 주문 당시 종목 이름
	private final String courseName;
	private final String participantName;
	private final String tshirtSize;
	private final ShippingAddressDto shippingAddress;
	private final KitDeliveryStatus kitDeliveryStatus;
	private final String kitCarrier;
	private final String kitTrackingNumber;
	private final LocalDateTime kitShippedAt;
	private final LocalDateTime kitDeliveredAt;

	// JPQL 생성자 조회용
	public SellerKitDto(int orderItemId, String orderNo, int raceId, int raceCourseId, String courseName,
		String participantName, String tshirtSize, ShippingAddress shippingAddress,
		KitDeliveryStatus kitDeliveryStatus, String kitCarrier, String kitTrackingNumber,
		LocalDateTime kitShippedAt, LocalDateTime kitDeliveredAt) {
		this.orderItemId = orderItemId;
		this.orderNo = orderNo;
		this.raceId = raceId;
		this.raceCourseId = raceCourseId;
		this.courseName = courseName;
		this.participantName = participantName;
		this.tshirtSize = tshirtSize;
		this.shippingAddress = ShippingAddressDto.from(shippingAddress);
		this.kitDeliveryStatus = kitDeliveryStatus;
		this.kitCarrier = kitCarrier;
		this.kitTrackingNumber = kitTrackingNumber;
		this.kitShippedAt = kitShippedAt;
		this.kitDeliveredAt = kitDeliveredAt;
	}
}
