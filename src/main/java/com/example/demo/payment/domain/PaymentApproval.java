package com.example.demo.payment.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * PG 승인 결과 — 결제 도메인의 언어로 번역된 뒤의 모양.
 *
 * @param approvedAt  승인 시각. 오프셋 정보를 잃지 않도록 Instant 로 받는다
 * @param requestedAt 결제 요청 시각(없을 수 있음)
 */
public record PaymentApproval(
        String paymentKey,
        String orderNo,
        long amount,
        PaymentMethod method,
        Instant approvedAt,
        Instant requestedAt
) {
    public PaymentApproval {
        Objects.requireNonNull(paymentKey, "paymentKey");
        Objects.requireNonNull(orderNo, "orderNo");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(approvedAt, "approvedAt");
        if (amount <= 0) {
            throw new IllegalArgumentException("승인 금액은 양수여야 합니다: " + amount);
        }
    }
}
