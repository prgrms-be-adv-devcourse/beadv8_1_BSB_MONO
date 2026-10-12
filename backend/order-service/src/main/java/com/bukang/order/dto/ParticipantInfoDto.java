package com.bukang.order.dto;

import java.time.LocalDate;

import com.bukang.order.domain.Gender;
import com.bukang.order.domain.ParticipantInfo;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 참가자 정보 응답. 참가 신청 조회와 다음 신청 때 채울 정보에 함께 쓴다
@AllArgsConstructor
@Getter
public class ParticipantInfoDto {
	private final String name;
	private final LocalDate birth;
	private final Gender gender;
	private final String phone;
	private final String emergencyContactName;
	private final String emergencyContactPhone;
	// 고르지 않았으면 null
	private final String tshirtSize;

	public static ParticipantInfoDto from(ParticipantInfo info) {
		return new ParticipantInfoDto(
			info.getName(),
			info.getBirth(),
			info.getGender(),
			info.getPhone(),
			info.getEmergencyContactName(),
			info.getEmergencyContactPhone(),
			info.getTshirtSize()
		);
	}
}
