package com.example.demo.order.infrastructure;

import com.example.demo.order.published.OrderSettlementFacts;
import com.example.demo.order.published.PaidOrderRow;
import com.example.demo.order.published.PaidOrderTotals;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * {@link OrderSettlementFacts} 구현. 목록과 통제 합계를 서로 다른 SQL 로 만든다 —
 * 합계를 목록에서 세면 "받은 것 = 보낸 것"을 스스로 증명하는 꼴이 되어 누락을 잡지 못한다.
 */
@Repository
public class JdbcOrderSettlementFacts implements OrderSettlementFacts {

    private final JdbcTemplate jdbc;

    public JdbcOrderSettlementFacts(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<PaidOrderRow> findPaidBetween(LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        return jdbc.query("""
                SELECT order_no, seller_id, gross_amount, paid_at
                FROM "order"
                WHERE paid_at >= ? AND paid_at < ?
                  AND gross_amount > 0
                ORDER BY order_no
                """,
                (rs, n) -> new PaidOrderRow(
                        rs.getString("order_no"),
                        rs.getObject("seller_id", UUID.class),
                        rs.getBigDecimal("gross_amount"),
                        rs.getObject("paid_at", LocalDateTime.class)),
                fromInclusive, toExclusive);
    }

    @Override
    public PaidOrderTotals totalsPaidBetween(LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) AS cnt, COALESCE(SUM(gross_amount), 0) AS total
                FROM "order"
                WHERE paid_at >= ? AND paid_at < ?
                  AND gross_amount > 0
                """,
                (rs, n) -> new PaidOrderTotals(rs.getLong("cnt"), rs.getBigDecimal("total")),
                fromInclusive, toExclusive);
    }
}
