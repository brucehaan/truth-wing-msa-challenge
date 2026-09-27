package com.example.demo.settlement.application.port.in;

import java.time.LocalDate;

public interface CloseDayUseCase {
    CloseResult close(LocalDate businessDate);

    record CloseResult(LocalDate date, boolean alreadyClosed, int statements) {}
}
