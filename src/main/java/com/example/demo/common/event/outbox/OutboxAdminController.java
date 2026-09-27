package com.example.demo.common.event.outbox;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 운영·시연용 — 아웃박스 적체를 보고, 스케줄러를 기다리지 않고 한 번 릴레이한다. */
@RestController
@RequestMapping("/internal/outbox")
@Tag(name = "Outbox", description = "아웃박스 운영 API (EDA)")
public class OutboxAdminController {

    private final OutboxStore store;
    private final OutboxRelay relay;
    private final int maxAttempts;

    public OutboxAdminController(OutboxStore store, OutboxRelay relay,
                                 @Value("${outbox.relay.max-attempts:10}") int maxAttempts) {
        this.store = store;
        this.relay = relay;
        this.maxAttempts = maxAttempts;
    }

    @GetMapping("/stats")
    @Operation(summary = "아웃박스 현황", description = "발행 대기 / 발행 완료 / 시도 한도 초과(dead) 건수")
    public ResponseEntity<OutboxStore.OutboxStats> stats() {
        return ResponseEntity.ok(store.stats(maxAttempts));
    }

    @PostMapping("/relay")
    @Operation(summary = "즉시 릴레이", description = "스케줄러 주기를 기다리지 않고 최대 limit 건을 발행한다")
    public ResponseEntity<OutboxRelay.Result> relayNow(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(relay.relay(limit));
    }
}
