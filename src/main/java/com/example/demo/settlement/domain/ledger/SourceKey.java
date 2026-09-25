package com.example.demo.settlement.domain.ledger;

import java.util.UUID;

public record SourceKey(
        String value
) {
    public SourceKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("sourceKey는 필수입니다");
        }
    }

    public static SourceKey sale(String orderNo) {
        return new SourceKey("SALE:" + orderNo);
    }
    public static SourceKey refund(String refundId) {
        return new SourceKey("REFUND:" + refundId);
    }
    public static SourceKey reversalOf(UUID journalId) {
        return new SourceKey("REVERSAL:" + journalId);
    }
}
