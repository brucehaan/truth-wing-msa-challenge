package com.example.demo.settlement.adapter.in;

import com.example.demo.settlement.application.port.in.RefundUseCase;
import com.example.demo.settlement.application.port.in.ReverseJournalUseCase;
import com.example.demo.settlement.domain.intake.RefundFact;
import com.example.demo.settlement.domain.ledger.JournalEntry;
import com.example.demo.settlement.domain.ledger.SourceKey;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 웹 명령의 트랜잭션 경계. 유스케이스는 스프링을 모르는 순수 자바라 @Transactional 을 달 수 없어서 입구가 연다.
 * (배치는 Step 트랜잭션이 경계라 이 파사드를 거치지 않는다.)
 */
@Service
public class SettlementCommandFacade {

    private final RefundUseCase refund;
    private final ReverseJournalUseCase reverseJournal;

    public SettlementCommandFacade(RefundUseCase refund, ReverseJournalUseCase reverseJournal) {
        this.refund = refund;
        this.reverseJournal = reverseJournal;
    }

    @Transactional
    public JournalEntry refund(RefundFact fact) {
        return refund.refund(fact);
    }

    @Transactional
    public JournalEntry reverse(SourceKey original, String operatorId) {
        return reverseJournal.reverse(original, operatorId);
    }
}
