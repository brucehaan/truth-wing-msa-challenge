package com.example.demo.settlement.adapter.in.web;

import com.example.demo.settlement.domain.closing.BusinessCalendar;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.example.demo.settlement.application.port.in.SettlementJobUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.http.HttpStatus.ACCEPTED;

@RestController
@RequestMapping("/api/batch/jobs")
@RequiredArgsConstructor
@Tag(name = "Batch", description = "정산 배치 실행 API")
public class BatchJobController {
    private final SettlementJobUseCase settlementJobUseCase;

    @PostMapping("/settlement-intake")
    @Operation(summary = "정산 수집 배치 실행", description = "targetDate 의 주문 사실을 스테이징 → 통제 합계 대조 → 원장 적재한다")
    public ResponseEntity<Map<String, Object>> runIntakeJob(
            @Parameter(description = "대상 거래일(yyyy-MM-dd), 미입력 시 어제(Asia/Seoul) — 정산 주기 D+1")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate targetDate,
            @Parameter(description = "실행 차수. 같은 날짜를 의도적으로 다시 수집할 때만 올린다")
            @RequestParam(defaultValue = "1")
            long attempt
    ) throws Exception {
        LocalDate date = resolve(targetDate);
        return ResponseEntity.status(ACCEPTED).body(toResponse(settlementJobUseCase.launchIntake(date, attempt), date, attempt));
    }

    @PostMapping("/settlement-close")
    @Operation(summary = "정산 마감 배치 실행", description = "targetDate 를 마감하고 판매자 정산서를 발행한다. 수집이 끝난 뒤에 돌린다")
    public ResponseEntity<Map<String, Object>> runCloseJob(
            @Parameter(description = "정산일(yyyy-MM-dd), 미입력 시 어제(Asia/Seoul)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate targetDate,
            @Parameter(description = "실행 차수")
            @RequestParam(defaultValue = "1")
            long attempt
    ) throws Exception {
        LocalDate date = resolve(targetDate);
        return ResponseEntity.status(ACCEPTED).body(toResponse(settlementJobUseCase.launchClose(date, attempt), date, attempt));
    }

    /* 서버 타임존이 아니라 영업 타임존 기준 어제 — D+1 에 D 를 정산한다 */
    private LocalDate resolve(LocalDate targetDate) {
        return targetDate != null ? targetDate : LocalDate.now(BusinessCalendar.ZONE).minusDays(1);
    }

    private Map<String, Object> toResponse(SettlementJobUseCase.JobResult result, LocalDate targetDate, long attempt) {
        return Map.of(
                "jobName", result.jobName(),
                "jobExecutionId", result.executionId(),
                "status", result.status(),
                "targetDate", targetDate.toString(),
                "attempt", attempt
        );
    }
}
