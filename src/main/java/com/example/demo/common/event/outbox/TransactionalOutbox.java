package com.example.demo.common.event.outbox;

import com.example.demo.common.event.EventRecorder;
import com.example.demo.common.event.IntegrationEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link EventRecorder} 구현 — 이벤트를 비즈니스 트랜잭션과 같은 트랜잭션으로 outbox_event 에 기록한다.
 *
 * <p>MANDATORY 인 이유: 트랜잭션 없이 호출되면 "상태는 롤백됐는데 이벤트만 남는" 불일치가 생긴다.
 * 그런 호출은 조용히 넘어가지 말고 즉시 실패시켜 개발 단계에서 드러나게 한다.</p>
 */
@Component
public class TransactionalOutbox implements EventRecorder {

    private final OutboxStore store;
    private final IntegrationEventCodec codec;

    public TransactionalOutbox(OutboxStore store, IntegrationEventCodec codec) {
        this.store = store;
        this.codec = codec;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(IntegrationEvent event) {
        store.append(new OutboxRecord(null, event.eventId(), event.eventType(), event.aggregateKey(),
                codec.encode(event), event.occurredAt(), 0));
    }
}
