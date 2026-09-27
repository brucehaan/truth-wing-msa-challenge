package com.example.demo.settlement.application.port.in;

import com.example.demo.settlement.domain.intake.RefundFact;
import com.example.demo.settlement.domain.ledger.JournalEntry;

public interface RefundUseCase {
    JournalEntry refund(RefundFact fact);
}
