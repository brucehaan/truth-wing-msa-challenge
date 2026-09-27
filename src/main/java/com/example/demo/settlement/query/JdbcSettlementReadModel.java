package com.example.demo.settlement.query;

import com.example.demo.settlement.query.view.SellerSummaryView;
import com.example.demo.settlement.query.view.SettlementDayView;
import com.example.demo.settlement.query.view.StatementView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/** settlement_read 스키마의 조회 모델. 조회 화면이 원하는 모양 그대로 저장해 조인·집계 없이 읽는다. */
@Repository
public class JdbcSettlementReadModel implements SettlementReadModel {

    private static final RowMapper<StatementView> STATEMENT = (rs, n) -> new StatementView(
            rs.getString("seller_id"),
            rs.getObject("business_date", LocalDate.class),
            rs.getLong("payable"),
            rs.getObject("closed_at", OffsetDateTime.class).toInstant());

    private final JdbcTemplate jdbc;

    public JdbcSettlementReadModel(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void upsertStatement(String sellerId, LocalDate businessDate, long payable, Instant closedAt) {
        jdbc.update("""
                INSERT INTO settlement_read.seller_statement_view (seller_id, business_date, payable, closed_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (seller_id, business_date) DO UPDATE
                    SET payable = EXCLUDED.payable, closed_at = EXCLUDED.closed_at, projected_at = now()
                """, sellerId, businessDate, payable, utc(closedAt));
    }

    @Override
    public void refreshSellerSummary(String sellerId) {
        jdbc.update("""
                INSERT INTO settlement_read.seller_summary_view
                    (seller_id, total_payable, statement_count, last_business_date)
                SELECT seller_id, SUM(payable), COUNT(*), MAX(business_date)
                FROM settlement_read.seller_statement_view
                WHERE seller_id = ?
                GROUP BY seller_id
                ON CONFLICT (seller_id) DO UPDATE
                    SET total_payable      = EXCLUDED.total_payable,
                        statement_count    = EXCLUDED.statement_count,
                        last_business_date = EXCLUDED.last_business_date,
                        updated_at         = now()
                """, sellerId);
    }

    @Override
    public void refreshDay(LocalDate businessDate, Instant closedAt) {
        jdbc.update("""
                INSERT INTO settlement_read.settlement_day_view AS v
                    (business_date, closed_at, statement_count, total_payable)
                SELECT CAST(? AS DATE), CAST(? AS TIMESTAMPTZ), COUNT(*), COALESCE(SUM(payable), 0)
                FROM settlement_read.seller_statement_view
                WHERE business_date = ?
                ON CONFLICT (business_date) DO UPDATE
                    SET closed_at       = COALESCE(EXCLUDED.closed_at, v.closed_at),
                        statement_count = EXCLUDED.statement_count,
                        total_payable   = EXCLUDED.total_payable,
                        updated_at      = now()
                """, businessDate, utc(closedAt), businessDate);
    }

    @Override
    public List<StatementView> statementsOf(String sellerId, LocalDate fromInclusive, LocalDate toInclusive) {
        return jdbc.query("""
                SELECT seller_id, business_date, payable, closed_at FROM settlement_read.seller_statement_view
                WHERE seller_id = ? AND business_date BETWEEN ? AND ?
                ORDER BY business_date
                """, STATEMENT, sellerId, fromInclusive, toInclusive);
    }

    @Override
    public Optional<SellerSummaryView> summaryOf(String sellerId) {
        return jdbc.query("""
                SELECT seller_id, total_payable, statement_count, last_business_date
                FROM settlement_read.seller_summary_view WHERE seller_id = ?
                """, (rs, n) -> new SellerSummaryView(
                        rs.getString("seller_id"),
                        rs.getLong("total_payable"),
                        rs.getInt("statement_count"),
                        rs.getObject("last_business_date", LocalDate.class)),
                sellerId).stream().findFirst();
    }

    @Override
    public Optional<SettlementDayView> day(LocalDate businessDate) {
        List<StatementView> statements = jdbc.query("""
                SELECT seller_id, business_date, payable, closed_at FROM settlement_read.seller_statement_view
                WHERE business_date = ? ORDER BY seller_id
                """, STATEMENT, businessDate);
        return jdbc.query("""
                SELECT business_date, closed_at, statement_count, total_payable
                FROM settlement_read.settlement_day_view WHERE business_date = ?
                """, (rs, n) -> {
                    OffsetDateTime closedAt = rs.getObject("closed_at", OffsetDateTime.class);
                    return new SettlementDayView(
                            rs.getObject("business_date", LocalDate.class),
                            closedAt == null ? null : closedAt.toInstant(),
                            rs.getInt("statement_count"),
                            rs.getLong("total_payable"),
                            statements);
                }, businessDate).stream().findFirst();
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
