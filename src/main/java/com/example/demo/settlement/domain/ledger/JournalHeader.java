package com.example.demo.settlement.domain.ledger;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * 전표 머리. businessDate는 "이 전표가 반영되는 정산일"이고,
 * occurredAt은 "원천 사실이 일어난 시각"이다. 둘은 다를 수 있다 (마감 후 도착분 이월)
 */
public record JournalHeader(
        JournalType type,
        SourceKey sourceKey,
        String orderNo,
        String sellerId,
        LocalDate businessDate,
        Instant occurredAt,
        UUID feePolicyId,
        UUID reversalOf,
        String issuedBy
) {
    public JournalHeader {
        if (issuedBy == null || issuedBy.isBlank()) {
            throw new IllegalArgumentException("issuedBy는 필수입니다 - 감사 추적");
        }
        if ((type == JournalType.SALE || type == JournalType.REFUND) && sellerId == null) {
            throw new IllegalArgumentException(type + " 전표는 sellerId가 필요합니다.");
        }
    }
}
