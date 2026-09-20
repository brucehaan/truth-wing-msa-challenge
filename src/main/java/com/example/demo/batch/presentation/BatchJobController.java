package com.example.demo.batch.presentation;

import com.example.demo.batch.application.SettlementJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/batch/jobs")
@RequiredArgsConstructor
@Tag(name = "Batch", description = "배치 실행 API")
public class BatchJobController {
    private final SettlementJobService settlementJobService;

    @PostMapping("/settlement")
    @Operation(summary = "정산 배치 실행", description = "settlementDate 기준으로 settlementJob을 실행한다")
    public ResponseEntity<Map<String, Object>> runSettlementJob(
            @Parameter(description = "정산일(yyyy-MM-dd), 미입력 시 오늘")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate settlementDate
    ) throws Exception {
        LocalDate targetDate = settlementDate == null ? LocalDate.now() : settlementDate;
        JobExecution execution = settlementJobService.launchTasklet(targetDate);
        return ResponseEntity.status(ACCEPTED).body(toResponse(execution, targetDate));
    }

    @PostMapping("/settlement-chunk")
    @Operation(summary = "정산 청크 배치 실행", description = "settlementDate 기준으로 settlementChunkJob을 실행한다")
    public ResponseEntity<Map<String, Object>> runSettlementChunkJob(
            @Parameter(description = "정산일(yyyy-MM-dd), 미입력 시 오늘")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate settlementDate
    ) throws Exception {
        LocalDate targetDate = settlementDate == null ? LocalDate.now() : settlementDate;
        JobExecution execution = settlementJobService.launchChunk(targetDate);
        return ResponseEntity.status(ACCEPTED).body(toResponse(execution, targetDate));
    }

    private Map<String, Object> toResponse(JobExecution execution, LocalDate targetDate) {
        return Map.of(
                "jobName", execution.getJobInstance().getJobName(),
                "jobExecutionId", execution.getId(),
                "status", execution.getStatus().toString(),
                "settlementDate", targetDate.toString()
        );
    }
}
