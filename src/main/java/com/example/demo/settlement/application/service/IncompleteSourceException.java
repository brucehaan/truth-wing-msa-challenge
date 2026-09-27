package com.example.demo.settlement.application.service;

import com.example.demo.settlement.domain.intake.ControlTotal;

import java.time.LocalDate;

public class IncompleteSourceException extends RuntimeException {
    public IncompleteSourceException(String source, LocalDate date, ControlTotal expected, ControlTotal actual) {
        super("원천 완결성 검증 실패. source=" + source + ", date=" + date + ", 상류 선언=" + expected + ", 수신=" + actual);
    }

    public IncompleteSourceException(String source, LocalDate date) {
        super("상류 매니페스트가 아직 없습니다. source=" + source + ", date=" + date);
    }
}
