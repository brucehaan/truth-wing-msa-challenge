package com.example.demo.payment.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토스 결제 완료 후 프론트에서 전달하는 승인 요청")
public record PaymentConfirmRequest(
        @Schema(description = "토스 결제 키")
        String paymentKey,

        @Schema(description = "주문 ID(토스에 전달했던 orderId)")
        String orderId,

        @Schema(description = "결제 금액")
        Long amount
) {
}
