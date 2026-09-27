package com.example.demo.settlement.domain.ledger;

import java.util.UUID;

/**
 * 멱등 키. 같은 원천 사실은 원장에 한 번만 들어간다.
 * 타임스탬프나 난수를 섞지 않는다 — 며칠 뒤 재실행해도 같은 키가 나와야 한다.
 */
public record SourceKey(String value) {

    public SourceKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("sourceKey 는 필수입니다");
        }
    }

    public static SourceKey sale(String orderNo)       { return new SourceKey("SALE:" + orderNo); }
    public static SourceKey refund(String refundId)    { return new SourceKey("REFUND:" + refundId); }
    public static SourceKey reversalOf(UUID journalId) { return new SourceKey("REVERSAL:" + journalId); }
}
