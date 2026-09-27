package com.example.demo.settlement.adapter.in.web;

import com.example.demo.settlement.application.port.in.RefundUseCase;
import com.example.demo.settlement.application.port.in.ReverseJournalUseCase;
import com.example.demo.settlement.domain.intake.RefundFact;
import com.example.demo.settlement.domain.ledger.JournalEntry;
import com.example.demo.settlement.domain.ledger.SourceKey;
import com.example.demo.settlement.domain.money.Money;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

/**
 * 정산 운영 API (README Step 11).
 * 유스케이스는 스프링을 모르는 순수 자바라 @Transactional 을 달 수 없다 → 트랜잭션 경계를 입구인 이 컨트롤러가 TransactionTemplate 으로 연다.
 */
@RestController
@RequestMapping("/internal/settlement")
@Tag(name = "Settlement Admin", description = "정산 운영 API — 정정·환불 전표")
public class SettlementAdminController {

    private final ReverseJournalUseCase reverseJournal;
    private final RefundUseCase refund;
    private final TransactionTemplate tx;
    private final Clock clock;

    public SettlementAdminController(ReverseJournalUseCase reverseJournal, RefundUseCase refund,
                                     PlatformTransactionManager transactionManager, Clock clock) {
        this.reverseJournal = reverseJournal;
        this.refund = refund;
        this.tx = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    /* 운영자 정정. 역할 인가는 게이트웨이 책임(1주차 Q7)이고, 여기서는 누가 했는지만 기록한다. */
    @PostMapping("/journals/{sourceKey}/reversal")
    @Operation(summary = "정정 전표", description = "잘못 적재된 전표를 반대 부호 전표로 상쇄한다. 원 전표는 남는다")
    public ResponseEntity<Map<String, Object>> reverse(@PathVariable String sourceKey,
                                                       @RequestHeader("X-Operator-Id") String operatorId) {
        JournalEntry rev = tx.execute(status -> reverseJournal.reverse(new SourceKey(sourceKey), operatorId));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "journalId", rev.id(),
                "businessDate", rev.header().businessDate().toString(),
                "reversalOf", rev.header().reversalOf()));
    }

    /* 환불 사실 등록 — 상류 환불 수집(파일·CDC)이 생기기 전까지 쓰는 입구. 같은 refundId 는 한 번만 반영된다. */
    @PostMapping("/refunds")
    @Operation(summary = "환불 전표", description = "원 판매 전표는 그대로 두고 반대 방향 전표를 추가한다")
    public ResponseEntity<Map<String, Object>> refund(@RequestBody RefundRequest request) {
        RefundFact fact = new RefundFact(request.refundId(), request.orderNo(), Money.won(request.amount()),
                request.occurredAt() == null ? clock.instant() : request.occurredAt());
        JournalEntry entry = tx.execute(status -> refund.refund(fact));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "journalId", entry.id(),
                "sourceKey", entry.header().sourceKey().value(),
                "businessDate", entry.header().businessDate().toString()));
    }

    public record RefundRequest(String refundId, String orderNo, long amount, Instant occurredAt) {
    }
}
