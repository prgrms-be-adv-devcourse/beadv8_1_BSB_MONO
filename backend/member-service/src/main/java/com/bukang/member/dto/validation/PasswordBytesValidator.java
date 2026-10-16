package com.bukang.member.dto.validation;

import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordBytesValidator implements ConstraintValidator<PasswordBytes, String> {
	// BCrypt가 받을 수 있는 최대 바이트 수
	private static final int MAX_BYTES = 72;

	@Override
	public boolean isValid(String password, ConstraintValidatorContext context) {
		// null 검사는 @NotBlank가 맡는다
		if (password == null) {
			return true;
		}
		return password.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
	}
}
