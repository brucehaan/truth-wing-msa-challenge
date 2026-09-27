package com.example.demo.settlement.query.view;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** 정산일 요약 + 그날 발행된 정산서. closedAt 이 null 이면 마감 이벤트가 아직 반영되지 않았다(최종적 일관성). */
public record SettlementDayView(LocalDate businessDate, Instant closedAt, int statementCount, long totalPayable,
                                List<StatementView> statements) {

    public boolean closed() {
        return closedAt != null;
    }
}
