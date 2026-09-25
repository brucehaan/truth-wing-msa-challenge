package com.example.demo.settlement.domain.intake;

import com.example.demo.settlement.domain.money.Money;

import java.time.Instant;
import java.util.Objects;

/**
 * 상류(주문)에서 넘어온 판매 사실 - 정산 언어로 번역된 뒤의 모양
 */
public record SaleFact(
        String orderNo,
        String paymentKey,
        String sellerId,
        Money gross,
        Instant occurredAt
) {
    public SaleFact {
        if (!gross.isPositive()) {
            throw new IllegalArgumentException("판매 금액은 양수여야 합니다.: " + gross);
        }
    }
}
