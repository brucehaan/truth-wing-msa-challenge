package com.example.demo.settlement.adapter.in.web;

import com.example.demo.settlement.adapter.in.SettlementCommandFacade;
import com.example.demo.settlement.adapter.in.web.dto.JournalResponse;
import com.example.demo.settlement.adapter.in.web.dto.RefundRequest;
import com.example.demo.settlement.domain.intake.RefundFact;
import com.example.demo.settlement.domain.ledger.SourceKey;
import com.example.demo.settlement.domain.money.Money;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

import static org.springframework.http.HttpStatus.CREATED;

/**
 * 정산 명령 API (CQRS — 쓰기 측, 운영자용). 수집·마감은 배치 API(BatchJobController)가, 조회는 settlement.query 가 맡는다.
 * 역할 인가는 게이트웨이 책임(1주차 Q7)이고, 여기서는 누가 했는지만 기록한다.
 */
@RestController
@RequestMapping("/internal/settlement")
@Tag(name = "Settlement Command", description = "정산 명령 API (CQRS 쓰기 측)")
public class SettlementCommandController {

    private final SettlementCommandFacade commands;
    private final Clock clock;

    public SettlementCommandController(SettlementCommandFacade commands, Clock clock) {
        this.commands = commands;
        this.clock = clock;
    }

    @PostMapping("/refunds")
    @Operation(summary = "환불 전표", description = "원 판매 전표는 그대로 두고 반대 방향 전표를 추가한다. 같은 refundId 는 한 번만 반영된다")
    public ResponseEntity<JournalResponse> refund(@RequestBody RefundRequest request) {
        RefundFact fact = new RefundFact(request.refundId(), request.orderNo(), Money.won(request.amount()),
                request.occurredAt() == null ? clock.instant() : request.occurredAt());
        return ResponseEntity.status(CREATED).body(JournalResponse.from(commands.refund(fact)));
    }

    @PostMapping("/journals/{sourceKey}/reversal")
    @Operation(summary = "정정 전표", description = "잘못 적재된 전표를 반대 부호 전표로 상쇄한다. 원 전표는 남는다")
    public ResponseEntity<JournalResponse> reverse(@PathVariable String sourceKey,
                                                   @RequestHeader("X-Operator-Id") String operatorId) {
        return ResponseEntity.status(CREATED).body(JournalResponse.from(commands.reverse(new SourceKey(sourceKey), operatorId)));
    }
}
