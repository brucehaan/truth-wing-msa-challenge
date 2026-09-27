package com.example.demo.settlement.adapter.in;

import com.example.demo.settlement.application.port.in.CloseDayUseCase;
import com.example.demo.settlement.application.port.in.CloseDayUseCase.CloseResult;
import com.example.demo.settlement.application.port.in.IngestSalesUseCase;
import com.example.demo.settlement.application.port.in.IngestSalesUseCase.IntakeResult;
import com.example.demo.settlement.application.port.in.RefundUseCase;
import com.example.demo.settlement.application.port.in.ReverseJournalUseCase;
import com.example.demo.settlement.application.port.in.StageSalesUseCase;
import com.example.demo.settlement.application.port.in.StageSalesUseCase.StageResult;
import com.example.demo.settlement.domain.intake.RefundFact;
import com.example.demo.settlement.domain.ledger.JournalEntry;
import com.example.demo.settlement.domain.ledger.SourceKey;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * CQRS 의 명령(Command) 측 입구이자 트랜잭션 경계.
 *
 * <p>유스케이스 서비스(application.service)는 스프링을 모르는 순수 자바라 {@code @Transactional} 을 달 수 없다.
 * 웹·배치 어댑터가 모두 이 파사드를 거치게 해서 "한 유스케이스 호출 = 한 트랜잭션"을 한곳에서 보장한다.
 * 마감처럼 이벤트를 남기는 명령은 원장·정산서 변경과 아웃박스 기록이 이 트랜잭션으로 함께 커밋된다.</p>
 */
@Service
public class SettlementCommandFacade {

    private final StageSalesUseCase stageSales;
    private final IngestSalesUseCase ingestSales;
    private final CloseDayUseCase closeDay;
    private final RefundUseCase refund;
    private final ReverseJournalUseCase reverseJournal;

    public SettlementCommandFacade(StageSalesUseCase stageSales, IngestSalesUseCase ingestSales, CloseDayUseCase closeDay,
                                   RefundUseCase refund, ReverseJournalUseCase reverseJournal) {
        this.stageSales = stageSales;
        this.ingestSales = ingestSales;
        this.closeDay = closeDay;
        this.refund = refund;
        this.reverseJournal = reverseJournal;
    }

    @Transactional
    public StageResult stage(LocalDate occurredDate) {
        return stageSales.stage(occurredDate);
    }

    @Transactional
    public IntakeResult ingest(LocalDate occurredDate) {
        return ingestSales.ingest(occurredDate);
    }

    @Transactional
    public CloseResult close(LocalDate businessDate) {
        return closeDay.close(businessDate);
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
