package com.example.demo.settlement.application.port.out;

import com.example.demo.settlement.domain.closing.SettlementDay;

import java.time.LocalDate;
import java.util.Optional;

public interface SettlementDayPort {
    Optional<SettlementDay> find(LocalDate date);
    void save(SettlementDay day);
}
