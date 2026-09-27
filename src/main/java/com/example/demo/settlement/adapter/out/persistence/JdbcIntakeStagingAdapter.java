package com.example.demo.settlement.adapter.out.persistence;

import com.example.demo.settlement.application.port.out.ManifestPort;
import com.example.demo.settlement.application.port.out.StagedFactPort;
import com.example.demo.settlement.application.port.out.StagingWriterPort;
import com.example.demo.settlement.application.service.IntakeService;
import com.example.demo.settlement.domain.intake.ControlTotal;
import com.example.demo.settlement.domain.intake.SaleFact;
import com.example.demo.settlement.domain.money.Money;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/** 수집 스테이징(inbound_sale_fact)과 상류 선언 통제 합계(inbound_manifest). */
@Repository
public class JdbcIntakeStagingAdapter implements StagedFactPort, ManifestPort, StagingWriterPort {

    private final JdbcTemplate jdbc;

    public JdbcIntakeStagingAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<SaleFact> stagedSales(LocalDate occurredDate) {
        return jdbc.query("""
                SELECT order_no, payment_key, seller_id, gross, occurred_at
                FROM settlement.inbound_sale_fact
                WHERE source = ? AND occurred_date = ?
                ORDER BY order_no
                """, (rs, n) -> new SaleFact(
                        rs.getString("order_no"),
                        rs.getString("payment_key"),
                        rs.getString("seller_id"),
                        Money.won(rs.getLong("gross")),
                        rs.getObject("occurred_at", OffsetDateTime.class).toInstant()),
                IntakeService.SOURCE, occurredDate);
    }

    /** 대규모에서 행을 메모리에 올리지 않도록 SQL 로 센다(포트의 기본 구현 재정의). */
    @Override
    public ControlTotal stagedTotal(LocalDate occurredDate) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) AS cnt, COALESCE(SUM(gross), 0) AS total
                FROM settlement.inbound_sale_fact
                WHERE source = ? AND occurred_date = ?
                """, (rs, n) -> new ControlTotal(rs.getLong("cnt"), Money.won(rs.getLong("total"))),
                IntakeService.SOURCE, occurredDate);
    }

    @Override
    public Optional<ControlTotal> manifest(String source, LocalDate occurredDate) {
        return jdbc.query("""
                SELECT record_count, amount_sum FROM settlement.inbound_manifest
                WHERE source = ? AND occurred_date = ?
                """, (rs, n) -> new ControlTotal(rs.getLong("record_count"), Money.won(rs.getLong("amount_sum"))),
                source, occurredDate).stream().findFirst();
    }

    /**
     * 그 날짜의 적재분을 통째로 바꾼다. 주문의 paidAt 이 바뀌어(예: PG 승인 시각으로 확정) 날짜가 옮겨간 주문은
     * PK(source, order_no) 충돌 시 새 날짜로 옮긴다 — 옛 날짜는 통제 합계가 어긋나 재수집 전까지 마감되지 않는다.
     */
    @Override
    public void replace(String source, LocalDate occurredDate, List<SaleFact> facts) {
        jdbc.update("DELETE FROM settlement.inbound_sale_fact WHERE source = ? AND occurred_date = ?", source, occurredDate);
        for (SaleFact f : facts) {
            jdbc.update("""
                    INSERT INTO settlement.inbound_sale_fact
                        (source, order_no, occurred_date, payment_key, seller_id, gross, occurred_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (source, order_no) DO UPDATE
                        SET occurred_date = EXCLUDED.occurred_date,
                            payment_key   = EXCLUDED.payment_key,
                            seller_id     = EXCLUDED.seller_id,
                            gross         = EXCLUDED.gross,
                            occurred_at   = EXCLUDED.occurred_at,
                            loaded_at     = now()
                    """, source, f.orderNo(), occurredDate, f.paymentKey(), f.sellerId(), f.gross().amount(),
                    f.occurredAt().atOffset(ZoneOffset.UTC));
        }
    }

    @Override
    public void declare(String source, LocalDate occurredDate, ControlTotal declared) {
        jdbc.update("""
                INSERT INTO settlement.inbound_manifest (source, occurred_date, record_count, amount_sum)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (source, occurred_date) DO UPDATE
                    SET record_count = EXCLUDED.record_count,
                        amount_sum   = EXCLUDED.amount_sum,
                        declared_at  = now()
                """, source, occurredDate, declared.count(), declared.sum().amount());
    }
}
