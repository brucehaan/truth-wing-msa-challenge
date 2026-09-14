package com.example.demo.payment.dto;

import com.example.demo.payment.domain.Payment;
import com.example.demo.payment.domain.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "결제 응답")
public record PaymentResponse(
        @Schema(description = "결제 ID(UUID)") // 질문 : 실무에서 MSA 아키텍처를 다룰 때 Long 타입을 쓰는 경우도 있는가? 있다면 UUID 쓰는 경우와 무슨 차이인가?
        UUID id,

        @Schema(description = "주문 ID")
        String orderId,

        @Schema(description = "토스 결제 키")
        String paymentKey,

        @Schema(description = "결제 금액")
        Long amount,

        @Schema(description = "결제 상태")
        PaymentStatus status,

        @Schema(description = "결제 수단")
        String method,

        @Schema(description = "결제 요청 시각")
        LocalDateTime requestedAt,

        @Schema(description = "결제 승인 시각")
        LocalDateTime approvedAt,

        @Schema(description = "실패 사유")
        String failReason
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getPaymentKey(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getMethod(),
                payment.getRequestedAt(),
                payment.getApprovedAt(),
                payment.getFailReason()
        );
    }
}
