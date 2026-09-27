package com.example.demo.settlement.query;

import com.example.demo.settlement.query.view.SellerSummaryView;
import com.example.demo.settlement.query.view.SettlementDayView;
import com.example.demo.settlement.query.view.StatementView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** 정산 조회 API (CQRS 읽기 측). 명령 API(SettlementCommandController)와 모델·저장소·서비스를 공유하지 않는다. */
@RestController
@Tag(name = "Settlement Query", description = "정산 조회 API (CQRS 읽기 측 — 조회 모델)")
public class SettlementQueryController {

    private final SettlementQueryService queries;

    public SettlementQueryController(SettlementQueryService queries) {
        this.queries = queries;
    }

    @GetMapping("/api/settlements/sellers/{sellerId}/statements")
    @Operation(summary = "판매자 정산서 목록", description = "기간 [from, to] 의 확정 정산서")
    public ResponseEntity<List<StatementView>> statements(
            @PathVariable String sellerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(queries.statements(sellerId, from, to));
    }

    @GetMapping("/api/settlements/sellers/{sellerId}/summary")
    @Operation(summary = "판매자 누적 요약", description = "지금까지 확정된 정산서의 합계")
    public ResponseEntity<SellerSummaryView> summary(@PathVariable String sellerId) {
        return queries.summary(sellerId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/settlements/days/{businessDate}")
    @Operation(summary = "정산일 현황", description = "마감 여부와 그날 발행된 정산서")
    public ResponseEntity<SettlementDayView> day(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate) {
        return queries.day(businessDate).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
