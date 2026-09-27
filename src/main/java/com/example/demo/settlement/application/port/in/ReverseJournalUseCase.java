package com.example.demo.settlement.application.port.in;

import com.example.demo.settlement.domain.ledger.JournalEntry;
import com.example.demo.settlement.domain.ledger.SourceKey;

public interface ReverseJournalUseCase {
    /** 잘못 적재된 전표를 정정한다. issuedBy 는 운영자 식별자 — 역할 인가는 인바운드 어댑터 책임. */
    JournalEntry reverse(SourceKey original, String issuedBy);
}
