package com.example.demo.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토스 결제 실패 콜백 요청")
public record PaymentFailRequest(
        @Schema(description = "주문 ID")
        String orderId,

        @Schema(description = "토스 결제 키")
        String paymentKey,

        @Schema(description = "토스 에러 코드")
        String code,

        @Schema(description = "토스 에러 메시지")
        String message,

        @Schema(description = "결제 금액")
        Long amount,

        @Schema(description = "원본 payload(JSON 문자열)")
        String rawPayload
) {
}
