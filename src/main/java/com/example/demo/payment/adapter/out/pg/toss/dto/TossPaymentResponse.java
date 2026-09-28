package com.example.demo.payment.adapter.out.pg.toss.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * 토스 결제 승인 API 응답(Payment 객체)의 일부 — 토스의 언어 그대로다. 이 타입은 ACL 밖으로 나가지 않는다.
 * method: "카드", "가상계좌", "간편결제", "휴대폰", "계좌이체", "문화상품권", "도서문화상품권", "게임문화상품권" 중 하나(nullable).
 * status: READY, IN_PROGRESS, WAITING_FOR_DEPOSIT, DONE, CANCELED, PARTIAL_CANCELED, ABORTED, EXPIRED.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TossPaymentResponse(
        String paymentKey,
        String orderId,
        @JsonProperty("totalAmount")
        Long totalAmount,
        String method,
        String status,
        OffsetDateTime requestedAt,
        OffsetDateTime approvedAt
) {
}
