package com.example.demo.settlement.published;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * 판매자 정산서가 발행됐다 — 마감 시 판매자 × 정산일마다 한 번.
 *
 * @param payable 확정된 지급 대상액(원). 환불이 판매보다 많으면 음수다
 */
public record SellerStatementIssued(
        UUID eventId,
        Instant occurredAt,
        String sellerId,
        LocalDate businessDate,
        long payable,
        Instant closedAt
) implements SettlementEvent {

    public SellerStatementIssued {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(sellerId, "sellerId");
        Objects.requireNonNull(businessDate, "businessDate");
        Objects.requireNonNull(closedAt, "closedAt");
    }

    public static SellerStatementIssued of(String sellerId, LocalDate businessDate, long payable, Instant closedAt) {
        return new SellerStatementIssued(UUID.randomUUID(), closedAt, sellerId, businessDate, payable, closedAt);
    }

    @Override
    public String aggregateKey() {
        return sellerId + ":" + businessDate;
    }
}
