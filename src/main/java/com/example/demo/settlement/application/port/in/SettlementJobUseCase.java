package com.example.demo.settlement.application.port.in;

import java.time.LocalDate;

/**
 * 정산 배치 실행 유스케이스 — 웹(BatchJobController)이 부르고, 구현(SettlementJobService)이 config 의 Job 을 실행한다.
 * Job · 파라미터 이름을 여기에 두는 이유: 애플리케이션이 config 를 import 하지 않게 하기 위해서다(설정 → 애플리케이션 방향만 허용).
 */
public interface SettlementJobUseCase {

    String INTAKE_JOB = "settlementIntakeJob";
    String CLOSE_JOB = "settlementCloseJob";

    /* 식별 파라미터는 targetDate + attempt — 스케줄러는 attempt=1, 의도적 재수집만 attempt 를 올린다 */
    String TARGET_DATE = "targetDate";
    String ATTEMPT = "attempt";

    JobResult launchIntake(LocalDate targetDate, long attempt) throws Exception;

    JobResult launchClose(LocalDate targetDate, long attempt) throws Exception;

    record JobResult(String jobName, Long executionId, String status) {
    }
}
