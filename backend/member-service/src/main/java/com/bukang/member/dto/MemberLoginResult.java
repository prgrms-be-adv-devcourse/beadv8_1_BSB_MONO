package com.bukang.member.dto;

/**
 * 로그인 결과. Refresh Token은 응답 본문이 아니라 쿠키로 내려주므로 컨트롤러에서 나눠 쓴다.
 */
public record MemberLoginResult(MemberDto member, AuthTokens tokens) {
}
