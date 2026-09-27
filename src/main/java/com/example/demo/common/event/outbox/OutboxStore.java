package com.example.demo.common.event.outbox;

import java.time.Instant;
import java.util.Optional;

/** 아웃박스 저장소 포트. 운영 구현은 {@link JdbcOutboxStore}, 테스트는 인메모리 구현을 쓴다. */
public interface OutboxStore {

    /** 비즈니스 트랜잭션 안에서 호출된다. id 는 저장소가 부여한다. */
    void append(OutboxRecord record);

    /**
     * 아직 발행되지 않았고, 시도 횟수가 한도 미만이며, 재시도 시각이 된 가장 오래된 행 하나를 잠그고 돌려준다.
     * 다른 릴레이 인스턴스가 이미 잠근 행은 건너뛴다(FOR UPDATE SKIP LOCKED) — 여러 대가 떠도 같은 행을 동시에 발행하지 않는다.
     */
    Optional<OutboxRecord> lockNextPending(Instant now, int maxAttempts);

    void markPublished(long id, Instant publishedAt);

    /** 시도 횟수를 1 올리고 오류와 다음 재시도 시각을 남긴다. */
    void markFailed(long id, String error, Instant nextAttemptAt);

    OutboxStats stats(int maxAttempts);

    /** pending = 발행 대기, dead = 시도 한도를 넘겨 사람이 봐야 하는 행 */
    record OutboxStats(long pending, long published, long dead) {
    }
}
