package com.example.demo.settlement.application.port.out;

import com.example.demo.settlement.domain.intake.ControlTotal;
import com.example.demo.settlement.domain.intake.SaleFact;

import java.time.LocalDate;
import java.util.List;

/** 스테이징에 적재된 상류 사실. 파일·CDC·gRPC Pull 어느 것으로 채웠든 이 포트는 같다. */
public interface StagedFactPort {

    List<SaleFact> stagedSales(LocalDate occurredDate);

    /**
     * 스테이징의 통제 합계. 기본 구현은 전부 읽어 세지만,
     * 대규모에서는 JDBC 어댑터가 SELECT COUNT(*), SUM(gross) 로 재정의해 메모리에 올리지 않는다.
     */
    default ControlTotal stagedTotal(LocalDate occurredDate) {
        return ControlTotal.of(stagedSales(occurredDate).stream().map(SaleFact::gross).toList());
    }
}
