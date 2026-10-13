package com.bukang.order.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "장바구니 결제 요청. 장바구니의 참가권마다 미리 작성한 참가 신청 ID를 보낸다")
@AllArgsConstructor
@Getter
public class CartCheckoutRequestDto {
	public static final int MAX_ITEMS = 10;

	@ArraySchema(arraySchema = @Schema(description = "결제할 참가 신청 ID 목록 (최대 10개, 겹치지 않게)"),
		schema = @Schema(example = "1"))
	@NotEmpty(message = "결제할 참가 신청을 선택해주세요.")
	@Size(max = MAX_ITEMS, message = "한 번에 10건까지 결제할 수 있습니다.")
	private final List<
		@NotNull(message = "참가 신청 ID가 올바르지 않습니다.")
		@Positive(message = "참가 신청 ID가 올바르지 않습니다.") Integer> participationIds;
}
