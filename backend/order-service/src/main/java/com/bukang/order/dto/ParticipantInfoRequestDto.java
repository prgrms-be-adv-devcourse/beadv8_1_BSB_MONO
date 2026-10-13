package com.bukang.order.dto;

import java.time.LocalDate;

import com.bukang.order.domain.Gender;
import com.bukang.order.domain.ParticipantInfo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "참가자 정보 요청 (참가 신청서 작성·수정에 함께 쓴다)")
@AllArgsConstructor
@Getter
public class ParticipantInfoRequestDto {
	static final String PHONE_REGEXP = "^01[016789]-?\\d{3,4}-?\\d{4}$";

	@Schema(description = "참가자 이름 (50자 이하)", example = "김러너")
	@NotBlank(message = "참가자 이름을 작성해주세요.")
	@Size(max = 50, message = "참가자 이름은 50자 이하로 작성해주세요.")
	private final String name;

	@Schema(description = "참가자 생년월일 (종목 나이 제한 확인에 쓴다)", example = "1994-06-25")
	@NotNull(message = "생년월일을 작성해주세요.")
	@Past(message = "생년월일은 오늘보다 이전이어야 합니다.")
	private final LocalDate birth;

	@Schema(description = "참가자 성별", example = "MALE")
	@NotNull(message = "성별을 선택해주세요.")
	private final Gender gender;

	@Schema(description = "참가자 휴대폰 번호 (하이픈 생략 가능)", example = "010-1234-5678")
	@NotBlank(message = "참가자 휴대폰 번호를 작성해주세요.")
	@Pattern(regexp = PHONE_REGEXP, message = "휴대폰 번호 형식에 맞게 작성해주세요.")
	private final String phone;

	@Schema(description = "비상 연락처 이름 (연락받을 사람에게 알린 뒤 입력, 50자 이하)", example = "김가족")
	@NotBlank(message = "비상 연락처 이름을 작성해주세요.")
	@Size(max = 50, message = "비상 연락처 이름은 50자 이하로 작성해주세요.")
	private final String emergencyContactName;

	@Schema(description = "비상 연락처 휴대폰 번호 (하이픈 생략 가능)", example = "010-9876-5432")
	@NotBlank(message = "비상 연락처 전화번호를 작성해주세요.")
	@Pattern(regexp = PHONE_REGEXP, message = "비상 연락처 전화번호 형식에 맞게 작성해주세요.")
	private final String emergencyContactPhone;

	@Schema(description = "기념 티셔츠(키트) 사이즈 (키트가 없는 종목이면 비워 둔다, 10자 이하)", example = "L")
	@Size(max = 10, message = "티셔츠 사이즈는 10자 이하로 작성해주세요.")
	private final String tshirtSize;

	@Schema(description = "키트 배송지. 키트가 있는 종목은 필수이고, 키트가 없는 종목은 보내지 않는다 (보내면 형식은 검사하고 저장하지 않는다)")
	@Valid
	private final ShippingAddressRequestDto shippingAddress;

	public ParticipantInfo toParticipantInfo() {
		return new ParticipantInfo(name.strip(), birth, gender, normalizePhone(phone), emergencyContactName.strip(),
			normalizePhone(emergencyContactPhone), blankToNull(tshirtSize),
			shippingAddress == null ? null : shippingAddress.toShippingAddress());
	}

	// 하이픈을 넣거나 빼고 보내도 같은 사람의 번호가 같은 값으로 저장되도록 010-1234-5678 형식으로 맞춘다
	static String normalizePhone(String value) {
		String digits = value.replace("-", "");
		int middleEnd = digits.length() - 4;
		return digits.substring(0, 3) + "-" + digits.substring(3, middleEnd) + "-" + digits.substring(middleEnd);
	}

	// 키트가 없는 종목은 사이즈를 비워 보낸다. 빈 문자열과 null이 섞이지 않게 null로 맞춘다
	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}
}
