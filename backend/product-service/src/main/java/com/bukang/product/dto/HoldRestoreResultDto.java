package com.bukang.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 판매한 자리를 정원으로 되돌린 결과
@AllArgsConstructor
@Getter
public class HoldRestoreResultDto {
	// 이번 요청으로 되돌렸는지. 이미 되돌렸거나 판매한 자리가 없으면 false (여러 번 불러도 한 번만 되돌린다)
	private final boolean restored;
}
