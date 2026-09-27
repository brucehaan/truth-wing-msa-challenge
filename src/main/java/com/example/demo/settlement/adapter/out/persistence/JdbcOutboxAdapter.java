package com.example.demo.settlement.adapter.out.persistence;

import com.example.demo.settlement.application.port.out.OutboxPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 트랜잭셔널 아웃박스 (README 3장 JdbcOutboxAdapter). 마감 트랜잭션 안에서 원장·정산서와 함께 커밋된다.
 * 발행(릴레이)은 이후 단계(조회 모델·지급) 몫이다 — published_at 이 비어 있는 행이 발행 대기 목록이다.
 */
@Repository
@RequiredArgsConstructor
public class JdbcOutboxAdapter implements OutboxPort {

    private final JdbcTemplate jdbc;

    @Override
    public void append(String eventType, String aggregateKey, String payload) {
        jdbc.update("""
                INSERT INTO settlement.outbox (event_type, aggregate_key, payload)
                VALUES (?, ?, CAST(? AS jsonb))
                """, eventType, aggregateKey, payload);
    }
}
