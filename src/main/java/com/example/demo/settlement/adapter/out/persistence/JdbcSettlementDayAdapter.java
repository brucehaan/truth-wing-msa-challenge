package com.example.demo.settlement.adapter.out.persistence;

import com.example.demo.settlement.application.port.out.SettlementDayPort;
import com.example.demo.settlement.domain.closing.DayStatus;
import com.example.demo.settlement.domain.closing.SettlementDay;
import com.example.demo.settlement.domain.intake.ControlTotal;
import com.example.demo.settlement.domain.money.Money;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 정산일 Aggregate 저장소. 원천별 검증 기록은 자식 테이블(settlement_day_source)에 둔다. */
@Repository
public class JdbcSettlementDayAdapter implements SettlementDayPort {

    private final JdbcTemplate jdbc;

    public JdbcSettlementDayAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<SettlementDay> find(LocalDate date) {
        List<DayRow> rows = jdbc.query(
                "SELECT status, closed_at FROM settlement.settlement_day WHERE business_date = ?",
                (rs, n) -> {
                    OffsetDateTime closedAt = rs.getObject("closed_at", OffsetDateTime.class);
                    return new DayRow(DayStatus.valueOf(rs.getString("status")), closedAt == null ? null : closedAt.toInstant());
                }, date);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Map<String, ControlTotal> verified = new HashMap<>();
        jdbc.query("""
                SELECT source, record_count, amount_sum FROM settlement.settlement_day_source
                WHERE business_date = ?
                """, (rs, n) -> verified.put(rs.getString("source"),
                        new ControlTotal(rs.getLong("record_count"), Money.won(rs.getLong("amount_sum")))), date);
        DayRow row = rows.get(0);
        return Optional.of(SettlementDay.restore(date, row.status(), verified, row.closedAt()));
    }

    /**
     * 마감된 날은 다시 열지 않는다 — ON CONFLICT ... WHERE status = 'OPEN' 이라 CLOSED 행은 갱신되지 않는다.
     * 마감 뒤의 재검증 기록(settlement_day_source)은 감사용으로 계속 남긴다.
     */
    @Override
    public void save(SettlementDay day) {
        jdbc.update("""
                INSERT INTO settlement.settlement_day AS d (business_date, status, closed_at) VALUES (?, ?, ?)
                ON CONFLICT (business_date) DO UPDATE
                    SET status = EXCLUDED.status, closed_at = EXCLUDED.closed_at
                    WHERE d.status = 'OPEN'
                """, day.date(), day.status().name(), utc(day.closedAt()));
        for (Map.Entry<String, ControlTotal> e : day.verifiedSources().entrySet()) {
            jdbc.update("""
                    INSERT INTO settlement.settlement_day_source (business_date, source, record_count, amount_sum)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT (business_date, source) DO UPDATE
                        SET record_count = EXCLUDED.record_count,
                            amount_sum   = EXCLUDED.amount_sum,
                            verified_at  = now()
                    """, day.date(), e.getKey(), e.getValue().count(), e.getValue().sum().amount());
        }
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private record DayRow(DayStatus status, Instant closedAt) {
    }
}
