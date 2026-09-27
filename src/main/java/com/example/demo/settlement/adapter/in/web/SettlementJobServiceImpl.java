package com.example.demo.settlement.adapter.in.web;

import com.example.demo.settlement.adapter.in.batch.BatchJobParameters;
import com.example.demo.settlement.adapter.in.batch.SettlementCloseJobConfig;
import com.example.demo.settlement.adapter.in.batch.SettlementIntakeJobConfig;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class SettlementJobServiceImpl implements SettlementJobService {

    private final JobOperator jobOperator;
    private final Job intakeJob;
    private final Job closeJob;

    public SettlementJobServiceImpl(
            JobOperator jobOperator,
            @Qualifier(SettlementIntakeJobConfig.JOB_NAME) Job intakeJob,
            @Qualifier(SettlementCloseJobConfig.JOB_NAME) Job closeJob) {
        this.jobOperator = jobOperator;
        this.intakeJob = intakeJob;
        this.closeJob = closeJob;
    }

    @Override
    public JobExecution launchIntake(LocalDate targetDate, long attempt) throws Exception {
        return jobOperator.start(intakeJob, buildJobParameters(targetDate, attempt));
    }

    @Override
    public JobExecution launchClose(LocalDate targetDate, long attempt) throws Exception {
        return jobOperator.start(closeJob, buildJobParameters(targetDate, attempt));
    }

    /* 식별 파라미터 = targetDate + attempt (BatchJobParameters 참고). 예전의 requestedAt(현재 시각)은 쓰지 않는다. */
    private JobParameters buildJobParameters(LocalDate targetDate, long attempt) {
        return new JobParametersBuilder()
                .addString(BatchJobParameters.TARGET_DATE, targetDate.toString())
                .addLong(BatchJobParameters.ATTEMPT, attempt)
                .toJobParameters();
    }
}
