package com.example.demo.settlement.adapter.out.persistence;

import com.example.demo.settlement.application.port.out.FeePolicyPort;
import com.example.demo.settlement.domain.fee.FeePolicy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** 수수료 정책 조회. 후보만 돌려주고, "무엇을 적용할지"는 도메인(FeePolicySelector)이 고른다. */
@Repository
public class JdbcFeePolicyAdapter implements FeePolicyPort {

    private static final RowMapper<FeePolicy> ROW = (rs, n) -> new FeePolicy(
            rs.getObject("id", UUID.class),
            rs.getString("seller_id"),
            rs.getBigDecimal("rate"),
            rs.getObject("effective_from", LocalDate.class),
            rs.getObject("effective_to", LocalDate.class),
            RoundingMode.valueOf(rs.getString("rounding")));

    private final JdbcTemplate jdbc;

    public JdbcFeePolicyAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<FeePolicy> candidates(String sellerId, LocalDate on) {
        return jdbc.query("""
                SELECT id, seller_id, rate, effective_from, effective_to, rounding
                FROM settlement.fee_policy
                WHERE (seller_id = ? OR seller_id IS NULL)
                  AND effective_from <= ?
                  AND (effective_to IS NULL OR effective_to > ?)
                """, ROW, sellerId, on, on);
    }

    @Override
    public FeePolicy getById(UUID policyId) {
        List<FeePolicy> found = jdbc.query("""
                SELECT id, seller_id, rate, effective_from, effective_to, rounding
                FROM settlement.fee_policy WHERE id = ?
                """, ROW, policyId);
        if (found.isEmpty()) {
            throw new IllegalStateException("수수료 정책이 없습니다: " + policyId);
        }
        return found.get(0);
    }
}
