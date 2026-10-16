package com.bukang.member.dto;

import java.time.LocalDate;
import java.util.List;

import com.bukang.member.domain.Gender;
import com.bukang.member.domain.TermsType;
import com.bukang.member.dto.validation.NicknameWidth;
import com.bukang.member.dto.validation.PasswordBytes;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "회원가입 요청")
@AllArgsConstructor
@Getter
public class MemberJoinRequestDto {
	// 255는 DB 칸 길이다 (SourceMember는 길이를 지정하지 않아 기본값 255)
	@Schema(description = "이메일 (로그인 ID, 255자 이하)", example = "runner1@crewrun.com")
	@NotBlank(message = "이메일을 작성해주세요.")
	@Email(message = "이메일 형식에 맞게 작성해주세요.")
	@Size(max = 255, message = "이메일은 255자 이하로 작성해주세요.")
	private final String email;

	// @Size는 null을 통과시키므로 @NotBlank를 함께 지정한다
	// BCrypt는 72바이트까지만 받으므로 글자 수가 아닌 바이트로 센다 (BE-018)
	@Schema(description = "비밀번호 (10자 이상, UTF-8 72바이트 이하. 한글은 한 글자가 3바이트)", example = "password1234")
	@NotBlank(message = "비밀번호를 작성해주세요.")
	@Size(min = 10, message = "비밀번호는 10자 이상 작성해야합니다.")
	@PasswordBytes
	private final String password;

	@Schema(description = "실명 (255자 이하)", example = "김러너")
	@NotBlank(message = "이름을 작성해주세요.")
	@Size(max = 255, message = "이름은 255자 이하로 작성해주세요.")
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

	// 만 14세 미만 검사는 MemberJoinUseCase에서 한다
	@Schema(description = "생년월일", example = "1995-04-12")
	@NotNull(message = "생년월일을 작성해주세요.")
	@Past(message = "생년월일은 오늘 이전 날짜여야 합니다.")
	private final LocalDate birthDate;

	@Schema(description = "성별 (선택)", example = "FEMALE", nullable = true)
	private final Gender gender;

	// 동의한 약관 종류. 필수 약관이 빠졌는지는 MemberJoinUseCase에서 확인한다 (BE-021)
	// 모르는 종류가 오면 JSON을 읽다 실패해 400 INVALID_REQUEST가 된다
	@Schema(description = "동의한 약관 종류 (SERVICE, PRIVACY는 필수). 동의하지 않은 선택 약관은 빼고 보낸다",
		example = "[\"SERVICE\", \"PRIVACY\", \"MARKETING\"]")
	@NotNull(message = "동의한 약관 목록을 작성해주세요.")
	private final List<TermsType> agreedTerms;
}
