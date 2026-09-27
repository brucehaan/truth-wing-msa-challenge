package com.example.demo.settlement.adapter.in.web;

import com.example.demo.settlement.adapter.in.SettlementCommandFacade;
import com.example.demo.settlement.adapter.in.web.dto.JournalResponse;
import com.example.demo.settlement.adapter.in.web.dto.RefundRequest;
import com.example.demo.settlement.application.port.in.CloseDayUseCase.CloseResult;
import com.example.demo.settlement.application.port.in.IngestSalesUseCase.IntakeResult;
import com.example.demo.settlement.application.port.in.StageSalesUseCase.StageResult;
import com.example.demo.settlement.domain.intake.RefundFact;
import com.example.demo.settlement.domain.ledger.SourceKey;
import com.example.demo.settlement.domain.money.Money;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;

import static org.springframework.http.HttpStatus.CREATED;

/**
 * 정산 명령 API (CQRS — Command). 상태를 바꾸는 요청만 받는다. 조회는 {@code settlement.query} 가 따로 맡는다.
 * 운영에서는 배치 Job(adapter.in.batch)이 같은 파사드로 stage → intake → close 를 돌린다. 이 API 는 수동 실행·시연용이다.
 */
@RestController
@Tag(name = "Settlement Command", description = "정산 명령 API (CQRS 쓰기 측)")
public class SettlementCommandController {

    private final SettlementCommandFacade commands;
    private final Clock clock;

    public SettlementCommandController(SettlementCommandFacade commands, Clock clock) {
        this.commands = commands;
        this.clock = clock;
    }

    @PostMapping("/api/settlements/days/{businessDate}/stage")
    @Operation(summary = "수집 준비", description = "주문 모듈(ACL 경유)에서 D일 판매 사실과 통제 합계를 스테이징에 싣는다")
    public ResponseEntity<StageResult> stage(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate) {
        return ResponseEntity.ok(commands.stage(businessDate));
    }

    @PostMapping("/api/settlements/days/{businessDate}/intake")
    @Operation(summary = "수집", description = "통제 합계 대조 후 판매 전표를 원장에 적재한다. 합계가 다르면 409, 한 건도 적재하지 않는다")
    public ResponseEntity<IntakeResult> intake(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate) {
        return ResponseEntity.ok(commands.ingest(businessDate));
    }

    @PostMapping("/api/settlements/days/{businessDate}/close")
    @Operation(summary = "마감", description = "판매자 정산서를 발행하고 SellerStatementIssued / SettlementDayClosed 이벤트를 아웃박스에 남긴다")
    public ResponseEntity<CloseResult> close(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate) {
        return ResponseEntity.ok(commands.close(businessDate));
    }

    @PostMapping("/api/settlements/refunds")
    @Operation(summary = "환불 전표", description = "원 판매 전표는 그대로 두고 반대 방향 전표를 추가한다. 같은 refundId 는 한 번만 반영된다")
    public ResponseEntity<JournalResponse> refund(@RequestBody RefundRequest request) {
        RefundFact fact = new RefundFact(request.refundId(), request.orderNo(), Money.won(request.amount()),
                request.occurredAt() == null ? clock.instant() : request.occurredAt());
        return ResponseEntity.status(CREATED).body(JournalResponse.from(commands.refund(fact)));
    }

    /** 운영자 정정. 역할 인가는 게이트웨이 책임(1주차 Q7)이고, 여기서는 누가 했는지만 기록한다. */
    @PostMapping("/internal/settlement/journals/{sourceKey}/reversal")
    @Operation(summary = "정정 전표", description = "잘못 적재된 전표를 반대 부호 전표로 상쇄한다(원 전표는 남는다)")
    public ResponseEntity<JournalResponse> reverse(@PathVariable String sourceKey,
                                                   @RequestHeader("X-Operator-Id") String operatorId) {
        return ResponseEntity.status(CREATED).body(JournalResponse.from(commands.reverse(new SourceKey(sourceKey), operatorId)));
    }
}
