package com.bukang.member.dto;

import java.time.LocalDate;

import com.bukang.member.dto.validation.NicknameWidth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 4칸 모두 필수로 받아 통째로 바꾼다. 검사 규칙은 가입(MemberJoinRequestDto)과 같다 (BE-012)
@Schema(description = "내 정보 수정 요청")
@AllArgsConstructor
@Getter
public class MemberModifyRequestDto {
	@Schema(description = "실명", example = "김러너")
	@NotBlank(message = "이름을 작성해주세요.")
	@Size(max = 255, message = "이름은 255자 이하로 작성해주세요.") // DB 칸 길이와 같다 (BE-018)
	private final String name;

	// 칸 수는 한글 2칸, 영문·숫자 1칸으로 센다 (BE-013)
	@Schema(description = "닉네임 (한글 2~8자, 영문 4~16자, 한글·영문·숫자만)", example = "러너1")
	@NotBlank(message = "닉네임을 작성해주세요.")
	@Pattern(regexp = "^[가-힣A-Za-z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 쓸 수 있습니다.")
	@NicknameWidth
	private final String nickname;

	// 010-1234-5678, 01012345678 형식 모두 허용
	@Schema(description = "휴대폰 번호 (하이픈 생략 가능)", example = "010-1234-5678")
	@NotBlank(message = "전화번호를 작성해주세요.")
	@Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "휴대폰 번호 형식에 맞게 작성해주세요.")
	private final String phone;

	// 만 14세 미만 검사는 MemberModifyUseCase에서 한다
	@Schema(description = "생년월일", example = "1995-04-12")
	@NotNull(message = "생년월일을 작성해주세요.")
	@Past(message = "생년월일은 오늘 이전 날짜여야 합니다.")
	private final LocalDate birthDate;
}
