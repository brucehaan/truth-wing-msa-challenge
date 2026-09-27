package com.example.demo.common.event.outbox;

import com.example.demo.common.event.IntegrationEvent;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 아웃박스 릴레이(Polling Publisher). 커밋된 이벤트를 꺼내 소비자에게 전달한다.
 *
 * <p>규칙</p>
 * <ol>
 *   <li>한 건 = 한 트랜잭션. "잠금 → 전달 → 발행 표시"가 한 트랜잭션이라, 같은 DB 를 쓰는 소비자의 반영과
 *       발행 표시가 함께 커밋되거나 함께 롤백된다</li>
 *   <li>전달이 실패하면 그 트랜잭션은 롤백되고(소비자의 반영도 취소), 별도 트랜잭션에서 시도 횟수와 다음 재시도 시각을 남긴다</li>
 *   <li>재시도 간격은 지수적으로 늘린다(상한 있음). 한도를 넘긴 이벤트는 더 꺼내지 않는다(dead) — 운영자가 본다</li>
 *   <li>실패한 이벤트가 뒤의 이벤트를 막지 않는다. 대신 실패 이후에는 이벤트 간 순서가 보장되지 않으므로,
 *       소비자는 순서에 기대지 않고 멱등하게 만든다</li>
 * </ol>
 */
public final class OutboxRelay {

    private final OutboxStore store;
    private final IntegrationEventCodec codec;
    private final EventDispatcher dispatcher;
    private final TxRunner tx;
    private final Clock clock;
    private final int maxAttempts;
    private final Duration baseBackoff;
    private final Duration maxBackoff;

    public OutboxRelay(OutboxStore store, IntegrationEventCodec codec, EventDispatcher dispatcher, TxRunner tx,
                       Clock clock, int maxAttempts, Duration baseBackoff, Duration maxBackoff) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts 는 1 이상이어야 합니다: " + maxAttempts);
        }
        this.store = Objects.requireNonNull(store);
        this.codec = Objects.requireNonNull(codec);
        this.dispatcher = Objects.requireNonNull(dispatcher);
        this.tx = Objects.requireNonNull(tx);
        this.clock = Objects.requireNonNull(clock);
        this.maxAttempts = maxAttempts;
        this.baseBackoff = Objects.requireNonNull(baseBackoff);
        this.maxBackoff = Objects.requireNonNull(maxBackoff);
    }

    /** 최대 limit 건을 발행한다. 보낼 것이 없으면 일찍 끝난다. */
    public Result relay(int limit) {
        int published = 0;
        int failed = 0;
        for (int i = 0; i < limit; i++) {
            AtomicReference<OutboxRecord> picked = new AtomicReference<>();
            try {
                boolean found = tx.inNewTransaction(() -> {
                    Optional<OutboxRecord> next = store.lockNextPending(clock.instant(), maxAttempts);
                    if (next.isEmpty()) {
                        return false;
                    }
                    OutboxRecord record = next.get();
                    picked.set(record);
                    IntegrationEvent event = codec.decode(record.eventType(), record.payload());
                    dispatcher.dispatch(event);
                    store.markPublished(record.id(), clock.instant());
                    return true;
                });
                if (!found) {
                    break;
                }
                published++;
            } catch (RuntimeException e) {
                OutboxRecord record = picked.get();
                if (record == null) {
                    throw e;                                   // 저장소 자체 장애 — 다음 주기에 다시 시도한다
                }
                Instant nextAttemptAt = clock.instant().plus(backoff(record.attempts() + 1));
                tx.inNewTransaction(() -> {
                    store.markFailed(record.id(), describe(e), nextAttemptAt);
                    return null;
                });
                failed++;
            }
        }
        return new Result(published, failed);
    }

    /** n 번째 실패 뒤의 대기 시간: base × 2^(n-1), 상한 maxBackoff. */
    Duration backoff(int attempts) {
        int exponent = Math.max(0, Math.min(attempts - 1, 20));
        Duration delay = baseBackoff.multipliedBy(1L << exponent);
        return delay.compareTo(maxBackoff) > 0 ? maxBackoff : delay;
    }

    private static String describe(RuntimeException e) {
        String message = e.getClass().getSimpleName() + ": " + e.getMessage();
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    public record Result(int published, int failed) {
    }
}
