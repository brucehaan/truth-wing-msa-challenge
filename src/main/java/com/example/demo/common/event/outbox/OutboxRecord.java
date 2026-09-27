package com.example.demo.common.event.outbox;

import java.time.Instant;
import java.util.UUID;

/** 아웃박스 테이블의 한 행. {@code id} 는 저장소가 부여하는 발행 순서다(새 행이면 null). */
public record OutboxRecord(
        Long id,
        UUID eventId,
        String eventType,
        String aggregateKey,
        String payload,
        Instant occurredAt,
        int attempts
) {
}
