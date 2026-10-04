package com.example.demo.settlement.application.service;

import com.example.demo.settlement.application.port.in.RefundUseCase;
import com.example.demo.settlement.application.port.in.ReverseJournalUseCase;
import com.example.demo.settlement.application.port.in.SettlementCommandUseCase;
import com.example.demo.settlement.domain.intake.RefundFact;
import com.example.demo.settlement.domain.ledger.JournalEntry;
import com.example.demo.settlement.domain.ledger.SourceKey;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영자 명령의 트랜잭션 경계. 도메인 유스케이스(RefundService · ReversalService)는 스프링을 모르는 순수 자바라,
 * 트랜잭션은 이 응용 서비스가 연다. (배치는 Job 의 Step 트랜잭션이 경계라 이 서비스를 거치지 않는다.)
 */
@Service
public class SettlementCommandService implements SettlementCommandUseCase {

    private final RefundUseCase refund;
    private final ReverseJournalUseCase reverseJournal;

    public SettlementCommandService(RefundUseCase refund, ReverseJournalUseCase reverseJournal) {
        this.refund = refund;
        this.reverseJournal = reverseJournal;
    }

    @Override
    @Transactional
    public JournalEntry refund(RefundFact fact) {
        return refund.refund(fact);
    }

    @Override
    @Transactional
    public JournalEntry reverse(SourceKey original, String operatorId) {
        return reverseJournal.reverse(original, operatorId);
    }
}
