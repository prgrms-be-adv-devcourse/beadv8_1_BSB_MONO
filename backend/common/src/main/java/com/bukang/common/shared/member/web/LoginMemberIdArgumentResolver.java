package com.bukang.common.shared.member.web;

import org.springframework.core.MethodParameter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @LoginMemberId 인자에 회원 ID 헤더 값을 넣는다
 * 오류는 BusinessException으로 던져 common GlobalExceptionHandler가 모든 서비스에서 같은 형식으로 응답하게 한다
 */
public class LoginMemberIdArgumentResolver implements HandlerMethodArgumentResolver {
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> type = parameter.getParameterType();
		return parameter.hasParameterAnnotation(LoginMemberId.class)
			&& (type == long.class || type == Long.class || type == int.class || type == Integer.class);
	}

	@Override
	public Object resolveArgument(
		MethodParameter parameter,
		ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest,
		WebDataBinderFactory binderFactory
	) {
		String header = webRequest.getHeader(MemberIdHeader.NAME);
		// 빈 값도 없는 것으로 본다 (@RequestHeader int가 빈 값을 헤더 누락으로 처리하던 것과 같다)
		if (!StringUtils.hasText(header)) {
			LoginMemberId loginMemberId = parameter.getParameterAnnotation(LoginMemberId.class);
			if (loginMemberId != null && !loginMemberId.required()) {
				return null;
			}
			throw new LoginRequiredException();
		}
		long memberId = parse(header.trim());
		if (isLong(parameter)) {
			return memberId;
		}
		// 회원 ID는 long으로 옮기는 중이다. 아직 int로 받는 인자는 int 범위를 넘으면 잘못된 값으로 본다
		if (memberId > Integer.MAX_VALUE) {
			throw new InvalidMemberIdException();
		}
		return (int)memberId;
	}

	private static boolean isLong(MethodParameter parameter) {
		Class<?> type = parameter.getParameterType();
		return type == long.class || type == Long.class;
	}

	private static long parse(String header) {
		long memberId;
		try {
			memberId = Long.parseLong(header);
		} catch (NumberFormatException exception) {
			throw new InvalidMemberIdException();
		}
		if (memberId <= 0) {
			throw new InvalidMemberIdException();
		}
		return memberId;
	}
}
