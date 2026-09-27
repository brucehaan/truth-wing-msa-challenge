package com.example.demo.settlement.adapter.in.web.dto;

import com.example.demo.settlement.domain.ledger.JournalEntry;

import java.time.LocalDate;
import java.util.UUID;

/** 전표 발행 결과. 도메인 객체를 그대로 노출하지 않고 필요한 값만 옮긴다. */
public record JournalResponse(UUID journalId, String type, String sourceKey, LocalDate businessDate, UUID reversalOf) {

    public static JournalResponse from(JournalEntry entry) {
        return new JournalResponse(entry.id(), entry.header().type().name(), entry.header().sourceKey().value(),
                entry.header().businessDate(), entry.header().reversalOf());
    }
}
