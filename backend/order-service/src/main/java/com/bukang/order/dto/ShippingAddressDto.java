package com.bukang.order.dto;

import com.bukang.order.domain.ShippingAddress;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 키트 배송지 응답. 참가 신청과 주문에 함께 쓴다
@AllArgsConstructor
@Getter
public class ShippingAddressDto {
	private final String recipientName;
	private final String recipientPhone;
	private final String zipcode;
	private final String address;
	// 없으면 null
	private final String addressDetail;

	// 배송지가 없으면(키트 없는 종목) null
	public static ShippingAddressDto from(ShippingAddress address) {
		if (address == null) {
			return null;
		}
		return new ShippingAddressDto(address.getRecipientName(), address.getRecipientPhone(), address.getZipcode(),
			address.getAddress(), address.getAddressDetail());
	}
}
