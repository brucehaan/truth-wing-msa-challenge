package com.example.demo.settlement.adapter.out.memory;

import com.example.demo.settlement.application.port.out.ManifestPort;
import com.example.demo.settlement.application.port.out.StagedFactPort;
import com.example.demo.settlement.domain.intake.ControlTotal;
import com.example.demo.settlement.domain.intake.SaleFact;

import java.time.LocalDate;
import java.util.*;

/**
 * 스테이징 + 매니페스트. 실제로는 파일 적재 어댑터나 CDC 싱크가 이 테이블을 채운다.
 */
public class InMemoryStaging implements StagedFactPort, ManifestPort {

    private final Map<LocalDate, List<SaleFact>> staged = new HashMap<>();
    private final Map<LocalDate, ControlTotal> manifests = new HashMap<>();

    public void stage(LocalDate date, SaleFact fact) {
        staged.computeIfAbsent(date, d -> new ArrayList<>()).add(fact);
    }

    /* 상류가 선언하는 통제 합계 */
    public void declare(LocalDate date, ControlTotal total) {
        manifests.put(date, total);
    }

    @Override
    public Optional<ControlTotal> manifest(String source, LocalDate occurredDate) {
        return Optional.ofNullable(manifests.get(occurredDate));
    }

    @Override
    public List<SaleFact> stagedSales(LocalDate occurredDate) {
        return List.copyOf(staged.getOrDefault(occurredDate, List.of()));
    }
}
