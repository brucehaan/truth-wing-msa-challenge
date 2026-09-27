package com.example.demo.settlement.application.service;

import com.example.demo.settlement.application.port.in.ReverseJournalUseCase;
import com.example.demo.settlement.application.port.out.LedgerPort;
import com.example.demo.settlement.application.port.out.SettlementDayPort;
import com.example.demo.settlement.domain.ledger.JournalEntry;
import com.example.demo.settlement.domain.ledger.JournalType;
import com.example.demo.settlement.domain.ledger.SourceKey;

import java.time.Clock;
import java.time.LocalDate;

public class ReversalService implements ReverseJournalUseCase {
    private final LedgerPort ledgerPort;
    private final PostingDateResolver postingDateResolver;
    private final Clock clock;

    public ReversalService(LedgerPort ledgerPort, SettlementDayPort dayPort, Clock clock) {
        this.ledgerPort = ledgerPort;
        this.postingDateResolver = new PostingDateResolver(dayPort, clock);
        this.clock = clock;
    }

    @Override
    public JournalEntry reverse(SourceKey originalKey, String issuedBy) {
        JournalEntry original = ledgerPort.findBySourceKey(originalKey)
                .orElseThrow(() -> new IllegalArgumentException("정정 대상 전표가 없습니다: " + originalKey.value()));
        if (original.header().type() == JournalType.REVERSAL) {
            throw new IllegalArgumentException("정정 전표를 다시 정정하지 않습니다. 원 사실을 재적재하세요.");
        }
        LocalDate postingDate = postingDateResolver.resolve(original.header().businessDate());
        JournalEntry reversal = original.reversal(postingDate, clock.instant(), issuedBy);
        if (!ledgerPort.appendIfAbsent(reversal)) {
            throw new IllegalStateException("이미 정정된 전표입니다: " + originalKey.value());
        }
        return reversal;
    }
}
