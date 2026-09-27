package com.example.demo.settlement.adapter.out.persistence;

import com.example.demo.settlement.application.port.out.SellerStatementPort;
import com.example.demo.settlement.application.port.out.SettlementDayPort;
import com.example.demo.settlement.domain.closing.DayStatus;
import com.example.demo.settlement.domain.closing.SellerStatement;
import com.example.demo.settlement.domain.closing.SettlementDay;
import com.example.demo.settlement.domain.intake.ControlTotal;
import com.example.demo.settlement.domain.money.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 마감 영속성 — 정산일(SettlementDay)과 판매자 정산서(SellerStatement). 인메모리 어댑터 InMemoryClosing 과 같은 역할을 DB 로 한다.
 * 정산서 PK(seller_id, business_date)가 중복 발행을 막는다 — 인메모리 어댑터가 예외를 던지는 것과 같은 의미.
 */
@Repository
@RequiredArgsConstructor
public class JdbcClosingAdapter implements SettlementDayPort, SellerStatementPort {

    private static final RowMapper<SellerStatement> STATEMENT = (rs, n) -> new SellerStatement(
            rs.getString("seller_id"),
            rs.getObject("business_date", LocalDate.class),
            Money.won(rs.getLong("payable")),
            rs.getTimestamp("closed_at").toInstant());

    private final JdbcTemplate jdbc;

    // ── SettlementDayPort ──────────────────────────────────────────────

    @Override
    public Optional<SettlementDay> find(LocalDate date) {
        List<DayRow> rows = jdbc.query(
                "SELECT status, closed_at FROM settlement.settlement_day WHERE business_date = ?",
                (rs, n) -> {
                    Timestamp closedAt = rs.getTimestamp("closed_at");
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
     * 마감된 날은 다시 열지 않는다 — ON CONFLICT ... WHERE d.status = 'OPEN' 이라 CLOSED 행은 갱신되지 않는다.
     * 마감 뒤의 재검증 기록(settlement_day_source)은 감사용으로 계속 남긴다.
     */
    @Override
    public void save(SettlementDay day) {
        jdbc.update("""
                INSERT INTO settlement.settlement_day AS d (business_date, status, closed_at) VALUES (?, ?, ?)
                ON CONFLICT (business_date) DO UPDATE
                    SET status = EXCLUDED.status, closed_at = EXCLUDED.closed_at
                    WHERE d.status = 'OPEN'
                """, day.date(), day.status().name(), day.closedAt() == null ? null : Timestamp.from(day.closedAt()));
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

    // ── SellerStatementPort ────────────────────────────────────────────

    @Override
    public Optional<SellerStatement> find(String sellerId, LocalDate businessDate) {
        return jdbc.query("""
                SELECT seller_id, business_date, payable, closed_at FROM settlement.seller_statement
                WHERE seller_id = ? AND business_date = ?
                """, STATEMENT, sellerId, businessDate).stream().findFirst();
    }

    @Override
    public void save(SellerStatement statement) {
        jdbc.update("""
                INSERT INTO settlement.seller_statement (seller_id, business_date, payable, closed_at)
                VALUES (?, ?, ?, ?)
                """, statement.sellerId(), statement.businessDate(), statement.payable().amount(),
                Timestamp.from(statement.closedAt()));
    }

    @Override
    public List<SellerStatement> findByDate(LocalDate businessDate) {
        return jdbc.query("""
                SELECT seller_id, business_date, payable, closed_at FROM settlement.seller_statement
                WHERE business_date = ? ORDER BY seller_id
                """, STATEMENT, businessDate);
    }

    private record DayRow(DayStatus status, Instant closedAt) {
    }
}
