package com.example.demo.common.event.outbox;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * processed_event 테이블로 구현한 멱등 소비자 기록.
 * 제약 위반 예외 대신 ON CONFLICT DO NOTHING 을 쓰는 이유: PostgreSQL 은 제약 위반이 나면 트랜잭션 전체를 abort 시켜
 * 같은 트랜잭션의 이후 문장(소비자의 실제 처리)이 모두 실패한다.
 */
@Repository
public class JdbcProcessedEvents implements ProcessedEvents {

    private final JdbcTemplate jdbc;

    public JdbcProcessedEvents(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean markIfFirst(String consumer, UUID eventId) {
        int inserted = jdbc.update("""
                INSERT INTO processed_event (consumer, event_id) VALUES (?, ?)
                ON CONFLICT (consumer, event_id) DO NOTHING
                """, consumer, eventId);
        return inserted == 1;
    }
}
