package com.example.demo.settlement.application.service;

import com.example.demo.settlement.application.port.in.SettlementJobUseCase;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/** 정산 배치 실행 — config 에 정의된 수집 · 마감 Job 을 Job 이름으로 받아 실행한다. */
@Service
public class SettlementJobService implements SettlementJobUseCase {

    private final JobOperator jobOperator;
    private final Job intakeJob;
    private final Job closeJob;

    public SettlementJobService(JobOperator jobOperator,
                                @Qualifier(INTAKE_JOB) Job intakeJob,
                                @Qualifier(CLOSE_JOB) Job closeJob) {
        this.jobOperator = jobOperator;
        this.intakeJob = intakeJob;
        this.closeJob = closeJob;
    }

    @Override
    public JobResult launchIntake(LocalDate targetDate, long attempt) throws Exception {
        return toResult(jobOperator.start(intakeJob, parameters(targetDate, attempt)));
    }

    @Override
    public JobResult launchClose(LocalDate targetDate, long attempt) throws Exception {
        return toResult(jobOperator.start(closeJob, parameters(targetDate, attempt)));
    }

    private static JobParameters parameters(LocalDate targetDate, long attempt) {
        return new JobParametersBuilder()
                .addString(TARGET_DATE, targetDate.toString())
                .addLong(ATTEMPT, attempt)
                .toJobParameters();
    }

    private static JobResult toResult(JobExecution execution) {
        return new JobResult(execution.getJobInstance().getJobName(), execution.getId(), execution.getStatus().toString());
    }
}
