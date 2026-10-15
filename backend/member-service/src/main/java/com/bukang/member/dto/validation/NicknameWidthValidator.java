package com.bukang.member.dto.validation;

import com.bukang.member.domain.Member;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NicknameWidthValidator implements ConstraintValidator<NicknameWidth, String> {
	@Override
	public boolean isValid(String nickname, ConstraintValidatorContext context) {
		// null 검사는 @NotBlank가 맡는다
		if (nickname == null) {
			return true;
		}
		int width = Member.nicknameWidth(nickname);
		return width >= Member.NICKNAME_MIN_WIDTH && width <= Member.NICKNAME_MAX_WIDTH;
	}
}
