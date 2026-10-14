package com.bukang.member.dto.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * 닉네임 칸 수가 4~16칸인지 검사한다 (한글 2칸, 그 밖의 글자 1칸, BE-013)
 * null은 통과시키므로 @NotBlank와 함께 쓴다
 */
@Documented
@Constraint(validatedBy = NicknameWidthValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NicknameWidth {
	String message() default "닉네임은 한글 2~8자, 영문 4~16자로 작성해야합니다.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
