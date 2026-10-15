package com.bukang.order.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QueueAdmissionDtoTest {
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 15, 11, 0);

	@Test
	@DisplayName("입장 끝 시각 전이면 입장 중이고 남은 초를 센다")
	void admitted() {
		QueueAdmissionDto dto = QueueAdmissionDto.of(NOW.plusSeconds(583), NOW);

		assertThat(dto.isAdmitted()).isTrue();
		assertThat(dto.getAdmissionEndsAt()).isEqualTo(NOW.plusSeconds(583));
		assertThat(dto.getRemainingSeconds()).isEqualTo(583);
	}

	@Test
	@DisplayName("끝 시각이 없거나 지금이거나 지났으면 입장 중이 아니고 남은 초는 0이다")
	void notAdmitted() {
		for (LocalDateTime endsAt : new LocalDateTime[] {null, NOW, NOW.minusSeconds(1)}) {
			QueueAdmissionDto dto = QueueAdmissionDto.of(endsAt, NOW);

			assertThat(dto.isAdmitted()).isFalse();
			assertThat(dto.getAdmissionEndsAt()).isNull();
			assertThat(dto.getRemainingSeconds()).isZero();
		}
	}
}
