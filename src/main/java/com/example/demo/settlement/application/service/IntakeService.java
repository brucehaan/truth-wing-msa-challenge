package com.example.demo.settlement.application.service;

import com.example.demo.settlement.application.port.in.IngestSalesUseCase;
import com.example.demo.settlement.application.port.out.*;
import com.example.demo.settlement.domain.closing.BusinessCalendar;
import com.example.demo.settlement.domain.closing.SettlementDay;
import com.example.demo.settlement.domain.fee.FeePolicy;
import com.example.demo.settlement.domain.fee.FeePolicySelector;
import com.example.demo.settlement.domain.intake.ControlTotal;
import com.example.demo.settlement.domain.intake.SaleFact;
import com.example.demo.settlement.domain.ledger.JournalFactory;
import com.example.demo.settlement.domain.ledger.SourceKey;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * 수집 유스케이스. 순서가 곧 규칙이다.
 * 1) 통제 합계로 완결성을 증명한다 - 실패하면 한 건도 적재하지 않는다.
 * 2) 멱등 키로 이미 적재된 건을 건너뛴다
 * 3) 수수료 정책은 거래일로 조회한다 - 반영 정산일이 이월돼도 금액은 같다
 * 4) 정산일에 원천 검증 완료를 기록한다 - 마감의 전제조건
 */
public class IntakeService implements IngestSalesUseCase {
    public static final String SOURCE = "ORDER";

    private final StagedFactPort stagedFactPort;
    private final ManifestPort manifestPort;
    private final LedgerPort ledgerPort;
    private final FeePolicyPort feePolicyPort;
    private final SettlementDayPort dayPort;
    private final JournalFactory journalFactory;
    private final FeePolicySelector policySelector;
    private final PostingDateResolver postingDateResolver;

    public IntakeService(StagedFactPort stagedFactPort, ManifestPort manifestPort, LedgerPort ledgerPort, FeePolicyPort feePolicyPort, SettlementDayPort dayPort, Clock clock) {
        this.stagedFactPort = stagedFactPort;
        this.manifestPort = manifestPort;
        this.ledgerPort = ledgerPort;
        this.feePolicyPort = feePolicyPort;
        this.dayPort = dayPort;
        this.journalFactory = new JournalFactory();
        this.policySelector = new FeePolicySelector();
        this.postingDateResolver = new PostingDateResolver(dayPort, clock);
    }

    /* 1) 완결성 게이트 - 상류가 선언한 통제 합계와 스테이징을 대조한다. */
    @Override
    public ControlTotal verifyCompleteness(LocalDate occurredDate) {
        ControlTotal received = stagedFactPort.stagedTotal(occurredDate); // 대규모: SQL 집계
        ControlTotal declared = manifestPort.manifest(SOURCE, occurredDate)
                .orElseThrow(() -> new IncompleteSourceException(SOURCE, occurredDate));
        if (!declared.equals(received)) {
            throw new IncompleteSourceException(SOURCE, occurredDate, declared, received);
        }
        return received;
    }

    /* 2) 청크단위 적재. 멱등하므로 청크가 재처리돼도 안전하다. */
    @Override
    public ChunkResult postChunk(List<SaleFact> facts, LocalDate occurredDate) {
        LocalDate postingDate = postingDateResolver.resolve(occurredDate);
        int posted = 0;
        int skipped = 0;
        for (SaleFact fact : facts) {
            SourceKey key = SourceKey.sale(fact.orderNo());
            if (ledgerPort.exists(key)) { // 1차 방어 - 정상 경로
                skipped++;
                continue;
            }
            LocalDate tradeDate = BusinessCalendar.dateOf(fact.occurredAt());
            FeePolicy policy = policySelector.select( // 거래일 기준
                    feePolicyPort.candidates(fact.sellerId(), tradeDate), fact.sellerId(), tradeDate
            );
            if (ledgerPort.appendIfAbsent(journalFactory.sale(fact, policy, postingDate))) {
                posted++;
            } else {
                skipped++; // 2차 방어 - 동시 실행이 1차를 통과한 경우
            }
        }
        return new ChunkResult(posted, skipped, postingDate);
    }

    /* 3) 검증 완료 기록 - 적재 도중 스테이징이 바뀌지 않았는지 다시 확인한 뒤 기록한다. 마감의 전제 조건 */
    @Override
    public void markVerified(LocalDate occurredDate) {
        ControlTotal received = verifyCompleteness(occurredDate);
        SettlementDay day = dayPort.find(occurredDate).orElseGet(() -> SettlementDay.open(occurredDate));
        day.recordVerifiedSource(SOURCE, received);
        dayPort.save(day);
    }

    @Override
    public IntakeResult ingest(LocalDate occurredDate) {
        verifyCompleteness(occurredDate); // 실패하면 한 건도 적재하지 않는다
        ChunkResult chunk = postChunk(stagedFactPort.stagedSales(occurredDate), occurredDate);
        markVerified(occurredDate);
        int routed = chunk.postingDate().equals(occurredDate) ? 0 : chunk.posted();
        return new IntakeResult(chunk.posted(), chunk.skipped(), routed);
    }
}
