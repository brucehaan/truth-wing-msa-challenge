package com.example.demo.settlement.published;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * 정산일이 마감됐다 — 그날 정산서가 모두 발행된 뒤 한 번.
 *
 * @param statementCount 이번 마감에서 새로 발행한 정산서 수
 */
public record SettlementDayClosed(
        UUID eventId,
        Instant occurredAt,
        LocalDate businessDate,
        Instant closedAt,
        int statementCount
) implements SettlementEvent {

    public SettlementDayClosed {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(businessDate, "businessDate");
        Objects.requireNonNull(closedAt, "closedAt");
    }

    public static SettlementDayClosed of(LocalDate businessDate, Instant closedAt, int statementCount) {
        return new SettlementDayClosed(UUID.randomUUID(), closedAt, businessDate, closedAt, statementCount);
    }

    @Override
    public String aggregateKey() {
        return businessDate.toString();
    }
}
