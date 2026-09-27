package com.example.demo.settlement.application.port.in;

import com.example.demo.settlement.domain.intake.ControlTotal;
import com.example.demo.settlement.domain.intake.SaleFact;

import java.time.LocalDate;
import java.util.List;

/**
 * 수집 유스케이스. 대규모 배치에서는 세 단계를 각각 다른 Step으로 돌린다.
 * verifyCompleteness -> (청크 반복) postChunk -> markVerified
 * ingest()는 셋을 한 번에 도는 편의 메서드다.
 */
public interface IngestSalesUseCase {
    ControlTotal verifyCompleteness(LocalDate occurredDate);

    ChunkResult postChunk(List<SaleFact> facts, LocalDate occurredDate);

    void markVerified(LocalDate occurredDate);

    IntakeResult ingest(LocalDate occurredDate);

    record ChunkResult(int posted, int skipped, LocalDate postingDate) {}

    record IntakeResult(int posted, int skipped, int routedToLaterDay) {}
}
