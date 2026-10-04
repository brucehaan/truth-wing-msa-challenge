package com.example.demo.settlement.config;

import com.example.demo.settlement.application.port.in.SettlementJobUseCase;
import org.springframework.batch.core.scope.context.ChunkContext;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * 정산 배치 Job 파라미터 규칙 (README Step 10).
 * 식별 파라미터는 targetDate + attempt 두 개다.
 *  - targetDate 하나만 두면 완료된 인스턴스를 같은 날짜로 다시 돌릴 수 없어 마감 후 누락분 재수집이 막힌다.
 *  - 예전처럼 requestedAt(현재 시각)을 넣으면 매번 새 인스턴스가 되어 중복 실행 방지가 꺼진다.
 *  → 스케줄러는 attempt=1 로 돌리고, 운영자의 의도적 재수집만 attempt 를 올린다.
 */
public final class BatchJobParameters {

    public static final String TARGET_DATE = SettlementJobUseCase.TARGET_DATE;
    public static final String ATTEMPT = SettlementJobUseCase.ATTEMPT;

    private BatchJobParameters() {
    }

    public static LocalDate targetDate(ChunkContext context) {
        Object raw = context.getStepContext().getJobParameters().get(TARGET_DATE);
        if (raw == null) {
            throw new IllegalArgumentException("Job 파라미터 " + TARGET_DATE + " 가 필요합니다 (yyyy-MM-dd)");
        }
        try {
            return LocalDate.parse(raw.toString());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(TARGET_DATE + " 는 yyyy-MM-dd 형식이어야 합니다: " + raw, e);
        }
    }
}
