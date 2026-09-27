package com.example.demo.common.event.outbox;

import java.util.UUID;

/**
 * 멱등 소비자(Idempotent Consumer) 포트.
 * 아웃박스 전달은 "적어도 한 번(at-least-once)"이므로 같은 이벤트가 다시 올 수 있다.
 * 소비자는 처리 전에 이 기록을 남기고, 이미 있으면 건너뛴다. 기록과 처리가 같은 트랜잭션이어야 의미가 있다.
 */
public interface ProcessedEvents {

    /** 처음 보는 (consumer, eventId) 면 기록하고 true, 이미 처리했으면 false. */
    boolean markIfFirst(String consumer, UUID eventId);
}
