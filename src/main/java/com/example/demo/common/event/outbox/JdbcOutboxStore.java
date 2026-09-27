package com.example.demo.common.event.outbox;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** PostgreSQL 아웃박스 저장소. 스키마는 resources/schema.sql 의 outbox_event. */
@Repository
public class JdbcOutboxStore implements OutboxStore {

    private static final RowMapper<OutboxRecord> ROW = (rs, n) -> new OutboxRecord(
            rs.getLong("id"),
            rs.getObject("event_id", UUID.class),
            rs.getString("event_type"),
            rs.getString("aggregate_key"),
            rs.getString("payload"),
            rs.getObject("occurred_at", OffsetDateTime.class).toInstant(),
            rs.getInt("attempts"));

    private final JdbcTemplate jdbc;

    public JdbcOutboxStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void append(OutboxRecord r) {
        jdbc.update("""
                INSERT INTO outbox_event (event_id, event_type, aggregate_key, payload, occurred_at)
                VALUES (?, ?, ?, CAST(? AS jsonb), ?)
                """, r.eventId(), r.eventType(), r.aggregateKey(), r.payload(), utc(r.occurredAt()));
    }

    @Override
    public Optional<OutboxRecord> lockNextPending(Instant now, int maxAttempts) {
        List<OutboxRecord> rows = jdbc.query("""
                SELECT id, event_id, event_type, aggregate_key, payload::text AS payload, occurred_at, attempts
                FROM outbox_event
                WHERE published_at IS NULL
                  AND attempts < ?
                  AND next_attempt_at <= ?
                ORDER BY id
                LIMIT 1
                FOR UPDATE SKIP LOCKED
                """, ROW, maxAttempts, utc(now));
        return rows.stream().findFirst();
    }

    @Override
    public void markPublished(long id, Instant publishedAt) {
        jdbc.update("UPDATE outbox_event SET published_at = ? WHERE id = ?", utc(publishedAt), id);
    }

    @Override
    public void markFailed(long id, String error, Instant nextAttemptAt) {
        jdbc.update("""
                UPDATE outbox_event
                SET attempts = attempts + 1, last_error = ?, next_attempt_at = ?
                WHERE id = ?
                """, error, utc(nextAttemptAt), id);
    }

    @Override
    public OutboxStats stats(int maxAttempts) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FILTER (WHERE published_at IS NULL AND attempts <  ?) AS pending,
                       COUNT(*) FILTER (WHERE published_at IS NOT NULL)               AS published,
                       COUNT(*) FILTER (WHERE published_at IS NULL AND attempts >= ?) AS dead
                FROM outbox_event
                """, (rs, n) -> new OutboxStats(rs.getLong("pending"), rs.getLong("published"), rs.getLong("dead")),
                maxAttempts, maxAttempts);
    }

    /** timestamptz 에는 오프셋이 명시된 값으로 바인딩한다 — JVM 기본 타임존에 기대지 않는다. */
    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
