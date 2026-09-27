package com.example.demo.settlement.adapter.out.persistence;

import com.example.demo.settlement.application.port.out.ManifestPort;
import com.example.demo.settlement.application.port.out.StagedFactPort;
import com.example.demo.settlement.application.service.IntakeService;
import com.example.demo.settlement.domain.intake.ControlTotal;
import com.example.demo.settlement.domain.intake.SaleFact;
import com.example.demo.settlement.domain.money.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 수집 스테이징(inbound_sale_fact)과 상류가 선언한 통제 합계(inbound_manifest)를 읽는다 (README Step 9).
 * 쓰기는 적재기(adapter/out/upstream/OrderStagingLoader)가 한다 — 나중에 적재기만 파일·CDC 로 바꾸면 이 클래스 아래는 그대로다.
 */
@Repository
@RequiredArgsConstructor
public class JdbcStagingAdapter implements StagedFactPort, ManifestPort {

    private final JdbcTemplate jdbc;

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
                        rs.getTimestamp("occurred_at").toInstant()),
                IntakeService.SOURCE, occurredDate);
    }

    /* 포트의 기본 구현(전부 읽어 세기)을 SQL 집계로 재정의한다 — 대규모에서 행을 메모리에 올리지 않는다 */
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
}
