package com.example.demo.settlement.config;

import com.example.demo.common.event.outbox.EventTypeRegistration;
import com.example.demo.settlement.application.port.in.CloseDayUseCase;
import com.example.demo.settlement.application.port.in.IngestSalesUseCase;
import com.example.demo.settlement.application.port.in.RefundUseCase;
import com.example.demo.settlement.application.port.in.ReverseJournalUseCase;
import com.example.demo.settlement.application.port.in.StageSalesUseCase;
import com.example.demo.settlement.application.port.out.FeePolicyPort;
import com.example.demo.settlement.application.port.out.LedgerPort;
import com.example.demo.settlement.application.port.out.ManifestPort;
import com.example.demo.settlement.application.port.out.OutboxPort;
import com.example.demo.settlement.application.port.out.SaleSourcePort;
import com.example.demo.settlement.application.port.out.SellerStatementPort;
import com.example.demo.settlement.application.port.out.SettlementDayPort;
import com.example.demo.settlement.application.port.out.StagedFactPort;
import com.example.demo.settlement.application.port.out.StagingWriterPort;
import com.example.demo.settlement.application.service.CloseService;
import com.example.demo.settlement.application.service.IntakeService;
import com.example.demo.settlement.application.service.RefundService;
import com.example.demo.settlement.application.service.ReversalService;
import com.example.demo.settlement.application.service.StagingService;
import com.example.demo.settlement.published.SellerStatementIssued;
import com.example.demo.settlement.published.SettlementDayClosed;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 정산 조립. 도메인·애플리케이션 클래스는 스프링 애너테이션이 없는 순수 자바라 여기서 빈으로 조립한다 —
 * 테스트의 SettlementFixture 가 인메모리 어댑터로 손수 하는 일을 스프링이 JDBC 어댑터로 한다.
 * Clock 은 config/ClockConfig 의 것 하나만 쓴다. 정산의 설정(이 조립 + 수집 · 마감 Job)은 settlement.config 한곳에 모은다.
 */
@Configuration
public class SettlementBeanConfig {

    @Bean
    public StageSalesUseCase stageSalesUseCase(SaleSourcePort source, StagingWriterPort staging) {
        return new StagingService(source, staging);
    }

    @Bean
    public IngestSalesUseCase ingestSalesUseCase(StagedFactPort staged, ManifestPort manifest, LedgerPort ledger,
                                                 FeePolicyPort policies, SettlementDayPort days, Clock clock) {
        return new IntakeService(staged, manifest, ledger, policies, days, clock);
    }

    @Bean
    public CloseDayUseCase closeDayUseCase(SettlementDayPort days, LedgerPort ledger,
                                           SellerStatementPort statements, OutboxPort outbox, Clock clock) {
        return new CloseService(days, ledger, statements, outbox, clock);
    }

    @Bean
    public RefundUseCase refundUseCase(LedgerPort ledger, FeePolicyPort policies, SettlementDayPort days, Clock clock) {
        return new RefundService(ledger, policies, days, clock);
    }

    @Bean
    public ReverseJournalUseCase reverseJournalUseCase(LedgerPort ledger, SettlementDayPort days, Clock clock) {
        return new ReversalService(ledger, days, clock);
    }

    /* 정산이 아웃박스로 발행하는 이벤트 타입 */
    @Bean
    public EventTypeRegistration settlementEventTypes() {
        return EventTypeRegistration.of(SellerStatementIssued.class, SettlementDayClosed.class);
    }
}
