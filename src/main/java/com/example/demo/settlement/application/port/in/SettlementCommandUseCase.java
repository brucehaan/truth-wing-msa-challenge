package com.example.demo.settlement.application.port.in;

import com.example.demo.settlement.domain.intake.RefundFact;
import com.example.demo.settlement.domain.ledger.JournalEntry;
import com.example.demo.settlement.domain.ledger.SourceKey;

/** 운영자 명령(환불 · 정정) — 웹이 부른다. 구현(SettlementCommandService)이 트랜잭션을 열고 도메인 유스케이스에 맡긴다. */
public interface SettlementCommandUseCase {

    JournalEntry refund(RefundFact fact);

    JournalEntry reverse(SourceKey original, String operatorId);
}
