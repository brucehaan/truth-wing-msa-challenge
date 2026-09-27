package com.example.demo.settlement.application.service;

import com.example.demo.settlement.application.port.in.StageSalesUseCase;
import com.example.demo.settlement.application.port.out.SaleSourcePort;
import com.example.demo.settlement.application.port.out.StagingWriterPort;
import com.example.demo.settlement.domain.intake.ControlTotal;
import com.example.demo.settlement.domain.intake.SaleFact;

import java.time.LocalDate;
import java.util.List;

/**
 * 수집 준비. 사실 목록과 통제 합계를 상류에서 "따로" 받아 그대로 싣는다.
 * 여기서 둘을 대조하지 않는다 — 대조는 수집(IntakeService.verifyCompleteness)의 책임이고,
 * 불일치하면 원장에 한 건도 들어가지 않는다.
 */
public class StagingService implements StageSalesUseCase {

    private final SaleSourcePort source;
    private final StagingWriterPort staging;

    public StagingService(SaleSourcePort source, StagingWriterPort staging) {
        this.source = source;
        this.staging = staging;
    }

    @Override
    public StageResult stage(LocalDate occurredDate) {
        List<SaleFact> facts = source.paidOn(occurredDate);
        ControlTotal declared = source.declaredTotal(occurredDate);
        staging.replace(IntakeService.SOURCE, occurredDate, facts);
        staging.declare(IntakeService.SOURCE, occurredDate, declared);
        return new StageResult(occurredDate, facts.size(), declared);
    }
}
