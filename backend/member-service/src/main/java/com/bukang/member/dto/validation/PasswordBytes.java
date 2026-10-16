package com.bukang.member.dto.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * 비밀번호가 UTF-8로 72바이트 이하인지 검사한다 (BE-018)
 * BCrypt는 72바이트까지만 받고 넘으면 예외를 던진다. 한글은 한 글자가 3바이트라 24자만 넘어도 걸린다
 * null은 통과시키므로 @NotBlank와 함께 쓴다
 */
@Documented
@Constraint(validatedBy = PasswordBytesValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordBytes {
	String message() default "비밀번호가 너무 깁니다.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
