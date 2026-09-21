package com.example.demo.payment.adapter.in.web.dto;

import com.example.demo.payment.domain.PaymentFailure;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "결제 실패 기록 응답")
public record PaymentFailureResponse(
        @Schema(description = "실패 기록 ID(UUID)")
        UUID id,

        @Schema(description = "주문 ID")
        String orderId,

        @Schema(description = "토스 결제 키")
        String paymentKey,

        @Schema(description = "토스 에러 코드")
        String errorCode,

        @Schema(description = "토스 에러 메시지")
        String errorMessage,

        @Schema(description = "결제 금액")
        Long amount,

        @Schema(description = "생성 일시")
        LocalDateTime createdAt
) {
    public static PaymentFailureResponse from(PaymentFailure failure) {
        return new PaymentFailureResponse(
                failure.getId(),
                failure.getOrderId(),
                failure.getPaymentKey(),
                failure.getErrorCode(),
                failure.getErrorMessage(),
                failure.getAmount(),
                failure.getCreatedAt()
        );
    }
}
