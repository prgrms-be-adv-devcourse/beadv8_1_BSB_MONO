package com.bukang.order.dto;

import com.bukang.order.domain.ShippingAddress;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "키트 배송지 요청 (ERD orders 배송지 칸과 같다)")
@AllArgsConstructor
@Getter
public class ShippingAddressRequestDto {
	private static final String ZIPCODE_REGEXP = "^\\d{5}$";

	@Schema(description = "받는 사람 (50자 이하)", example = "김러너")
	@NotBlank(message = "받는 사람을 작성해주세요.")
	@Size(max = 50, message = "받는 사람은 50자 이하로 작성해주세요.")
	private final String recipientName;

	@Schema(description = "받는 사람 휴대폰 번호 (하이픈 생략 가능)", example = "010-1234-5678")
	@NotBlank(message = "받는 사람 연락처를 작성해주세요.")
	@Pattern(regexp = ParticipantInfoRequestDto.PHONE_REGEXP, message = "받는 사람 연락처 형식에 맞게 작성해주세요.")
	private final String recipientPhone;

	@Schema(description = "우편번호 (숫자 5자리)", example = "04700")
	@NotBlank(message = "우편번호를 작성해주세요.")
	@Pattern(regexp = ZIPCODE_REGEXP, message = "우편번호는 숫자 5자리로 작성해주세요.")
	private final String zipcode;

	@Schema(description = "주소 (255자 이하)", example = "서울 성동구 왕십리로 00")
	@NotBlank(message = "주소를 작성해주세요.")
	@Size(max = 255, message = "주소는 255자 이하로 작성해주세요.")
	private final String address;

	@Schema(description = "상세 주소 (없으면 비워 둔다, 255자 이하)", example = "101동 1001호")
	@Size(max = 255, message = "상세 주소는 255자 이하로 작성해주세요.")
	private final String addressDetail;

	public ShippingAddress toShippingAddress() {
		String detail = addressDetail == null || addressDetail.isBlank() ? null : addressDetail.strip();
		return new ShippingAddress(recipientName.strip(), ParticipantInfoRequestDto.normalizePhone(recipientPhone),
			zipcode, address.strip(), detail);
	}
}
