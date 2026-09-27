package com.example.demo.settlement.application.port.in;

import com.example.demo.settlement.domain.intake.ControlTotal;

import java.time.LocalDate;

/**
 * 수집 준비 유스케이스 — 상류(주문)의 D일 판매 사실과 상류가 선언한 통제 합계를 스테이징에 싣는다.
 * 원장에는 아무것도 쓰지 않는다. 원장 적재는 {@link IngestSalesUseCase} 가 통제 합계 대조를 통과한 뒤에 한다.
 */
public interface StageSalesUseCase {

    StageResult stage(LocalDate occurredDate);

    record StageResult(LocalDate occurredDate, int staged, ControlTotal declared) {}
}
