package com.example.demo.settlement.domain.intake;

import com.example.demo.settlement.domain.money.Money;

import java.time.Instant;
import java.util.Objects;

/**
 * 환불 사실. 부분 환불은 건별로 별개의 refundId를 가진다.
 */
public record RefundFact(
        String refundId,
        String orderNo,
        Money amount,
        Instant occurredAt
) {
    public RefundFact {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("환불 금액은 양수여야 합니다: " + amount);
        }
    }
}
