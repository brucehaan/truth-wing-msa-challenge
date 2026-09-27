package com.example.demo.settlement.application.port.out;

import com.example.demo.settlement.domain.intake.ControlTotal;

import java.time.LocalDate;
import java.util.Optional;

/** 상류가 선언한 통제 합계. "D일 주문은 N건, 총 X원"을 상류가 직접 서명한 값이어야 의미가 있다. */
public interface ManifestPort {
    Optional<ControlTotal> manifest(String source, LocalDate occurredDate);
}
