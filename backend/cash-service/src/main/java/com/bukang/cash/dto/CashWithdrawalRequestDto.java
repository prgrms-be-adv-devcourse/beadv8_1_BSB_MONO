package com.bukang.cash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "예치금 전액 환급 신청")
public class CashWithdrawalRequestDto {
	@Schema(description = "은행 코드", example = "004")
	@NotBlank(message = "은행 코드를 작성해주세요.")
	@Size(max = 10, message = "은행 코드는 10자 이하여야 합니다.")
	private final String bankCode;

	@Schema(description = "계좌번호", example = "110123456789")
	@NotBlank(message = "계좌번호를 작성해주세요.")
	@Size(max = 255, message = "계좌번호는 255자 이하여야 합니다.")
	private final String accountNumber;

	@Schema(description = "예금주명", example = "홍길동")
	@NotBlank(message = "예금주명을 작성해주세요.")
	@Size(max = 50, message = "예금주명은 50자 이하여야 합니다.")
	private final String accountHolder;

	@Schema(description = "환급 신청 고유 ID (멱등키)", example = "withdrawal-1697000000000")
	@NotBlank(message = "idempotencyKey를 작성해주세요.")
	@Size(max = 64, message = "idempotencyKey는 64자 이하여야 합니다.")
	private final String idempotencyKey;
}
