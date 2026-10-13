package com.bukang.payout.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 페이지 응답. Spring Data의 Page를 그대로 JSON으로 내보내면 필드 모양이 버전마다 바뀌고 경고가 나므로, 쓰는 값만 고정해 내보낸다
 * 다른 서비스도 페이지 응답이 필요해지면 common으로 올린다
 */
@AllArgsConstructor
@Getter
public class PageDto<T> {
	private final List<T> content;
	// 0부터 센다
	private final int page;
	private final int size;
	private final long totalElements;
	private final int totalPages;

	public static <E, T> PageDto<T> from(Page<E> page, Function<E, T> mapper) {
		return new PageDto<>(
			page.getContent().stream().map(mapper).toList(),
			page.getNumber(),
			page.getSize(),
			page.getTotalElements(),
			page.getTotalPages()
		);
	}
}
