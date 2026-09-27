package com.example.demo.common.event.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 주기적으로 릴레이를 돌린다. 여러 인스턴스가 동시에 돌아도 SKIP LOCKED 로 같은 행을 두 번 잡지 않는다. */
@Component
public class OutboxRelayScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayScheduler.class);

    private final OutboxRelay relay;
    private final int batchSize;

    public OutboxRelayScheduler(OutboxRelay relay, @Value("${outbox.relay.batch-size:100}") int batchSize) {
        this.relay = relay;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.fixed-delay-ms:1000}")
    public void relayPending() {
        OutboxRelay.Result result = relay.relay(batchSize);
        if (result.failed() > 0) {
            log.warn("아웃박스 릴레이: 발행 {}건, 실패 {}건 (실패 건은 백오프 후 재시도)", result.published(), result.failed());
        } else if (result.published() > 0) {
            log.debug("아웃박스 릴레이: 발행 {}건", result.published());
        }
    }
}
