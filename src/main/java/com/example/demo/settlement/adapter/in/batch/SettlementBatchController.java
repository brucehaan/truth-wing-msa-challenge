package com.example.demo.settlement.adapter.in.batch;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.batch.core.job.JobExecution;
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
@Tag(name = "Batch", description = "배치 실행 API")
public class SettlementBatchController {

    private final SettlementJobLauncher launcher;

    public SettlementBatchController(SettlementJobLauncher launcher) {
        this.launcher = launcher;
    }

    @PostMapping("/daily-settlement")
    @Operation(summary = "일일 정산 배치 실행", description = "businessDate 의 stage → intake → close 를 한 Job 으로 실행한다")
    public ResponseEntity<Map<String, Object>> run(
            @Parameter(description = "정산일(yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate
    ) throws Exception {
        JobExecution execution = launcher.launch(businessDate);
        return ResponseEntity.status(ACCEPTED).body(Map.of(
                "jobName", execution.getJobInstance().getJobName(),
                "jobExecutionId", execution.getId(),
                "status", execution.getStatus().toString(),
                "businessDate", businessDate.toString()));
    }
}
