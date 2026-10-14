package com.bukang.member.out;

/**
 * 재발급 판정 결과.
 *
 * @param currentToken GRACE일 때만 있는, 지금 유효한 Refresh Token 원본. 그 밖에는 null
 */
public record RotationResult(RotationOutcome outcome, String currentToken) {

	// record 기본 toString()은 모든 값을 출력하므로, 실수로 로그에 찍혀도 토큰이 새지 않게 가린다
	@Override
	public String toString() {
		return "RotationResult[outcome=" + outcome + ", currentToken=" + (currentToken == null ? "null" : "***") + "]";
	}
}
