package com.example.demo.settlement.adapter.out.persistence;

import com.example.demo.settlement.application.port.out.FeePolicyPort;
import com.example.demo.settlement.domain.fee.FeePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** 수수료 정책 조회. 후보만 돌려주고, 무엇을 적용할지는 도메인(FeePolicySelector)이 고른다. */
@Repository
@RequiredArgsConstructor
public class JdbcFeePolicyAdapter implements FeePolicyPort {

    private static final RowMapper<FeePolicy> ROW = (rs, n) -> new FeePolicy(
            rs.getObject("id", UUID.class),
            rs.getString("seller_id"),
            rs.getBigDecimal("rate"),
            rs.getObject("effective_from", LocalDate.class),
            rs.getObject("effective_to", LocalDate.class),
            RoundingMode.valueOf(rs.getString("rounding")));

    private final JdbcTemplate jdbc;

    /* 반열림 [effective_from, effective_to) — 교체일에 두 정책이 동시에 유효해지지 않는다 */
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
        return jdbc.query("""
                SELECT id, seller_id, rate, effective_from, effective_to, rounding
                FROM settlement.fee_policy WHERE id = ?
                """, ROW, policyId).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("수수료 정책이 없습니다: " + policyId));
    }
}
