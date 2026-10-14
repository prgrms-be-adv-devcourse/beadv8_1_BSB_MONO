package com.bukang.product.domain;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * 티셔츠 사이즈 목록을 쉼표로 이어 한 칸에 저장한다 (예: [S, M, L] ↔ "S,M,L")
 * 빈 목록은 NULL로 저장하고 NULL은 빈 목록으로 읽는다.
 */
@Converter
public class TshirtSizeListConverter implements AttributeConverter<List<TshirtSize>, String> {
	private static final String DELIMITER = ",";

	@Override
	public String convertToDatabaseColumn(List<TshirtSize> sizes) {
		if (sizes == null || sizes.isEmpty()) {
			return null;
		}
		return sizes.stream().map(TshirtSize::name).collect(Collectors.joining(DELIMITER));
	}

	@Override
	public List<TshirtSize> convertToEntityAttribute(String column) {
		if (column == null || column.isBlank()) {
			return List.of();
		}
		return Arrays.stream(column.split(DELIMITER)).map(TshirtSize::valueOf).toList();
	}
}
