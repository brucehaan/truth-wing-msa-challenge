package com.example.demo.settlement.adapter.out.persistence;

import com.example.demo.settlement.application.port.out.SellerStatementPort;
import com.example.demo.settlement.domain.closing.SellerStatement;
import com.example.demo.settlement.domain.money.Money;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/** 판매자 정산서(쓰기 모델). 한 번 발행되면 바뀌지 않는다 — PK(seller_id, business_date)가 중복 발행을 막는다. */
@Repository
public class JdbcSellerStatementAdapter implements SellerStatementPort {

    private static final RowMapper<SellerStatement> ROW = (rs, n) -> new SellerStatement(
            rs.getString("seller_id"),
            rs.getObject("business_date", LocalDate.class),
            Money.won(rs.getLong("payable")),
            rs.getObject("closed_at", OffsetDateTime.class).toInstant());

    private final JdbcTemplate jdbc;

    public JdbcSellerStatementAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<SellerStatement> find(String sellerId, LocalDate businessDate) {
        return jdbc.query("""
                SELECT seller_id, business_date, payable, closed_at FROM settlement.seller_statement
                WHERE seller_id = ? AND business_date = ?
                """, ROW, sellerId, businessDate).stream().findFirst();
    }

    @Override
    public void save(SellerStatement s) {
        jdbc.update("""
                INSERT INTO settlement.seller_statement (seller_id, business_date, payable, closed_at)
                VALUES (?, ?, ?, ?)
                """, s.sellerId(), s.businessDate(), s.payable().amount(), s.closedAt().atOffset(ZoneOffset.UTC));
    }

    @Override
    public List<SellerStatement> findByDate(LocalDate businessDate) {
        return jdbc.query("""
                SELECT seller_id, business_date, payable, closed_at FROM settlement.seller_statement
                WHERE business_date = ? ORDER BY seller_id
                """, ROW, businessDate);
    }
}
