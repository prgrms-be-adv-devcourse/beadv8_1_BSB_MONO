package com.bukang.order.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CartItemDto {
	private final int id;
	// 담은 일시
	private final LocalDateTime createDate;
	private final int raceCourseId;
}
