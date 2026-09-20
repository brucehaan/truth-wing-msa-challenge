package com.example.demo.batch.application;

import com.example.demo.batch.config.SettlementBatchConfig;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class SettlementJobServiceImpl implements SettlementJobService {

    private final JobOperator jobOperator;
    private final Job settlementTaskletJob;
    private final Job settlementChunkJob;

    public SettlementJobServiceImpl(
            JobOperator jobOperator,
            @Qualifier(SettlementBatchConfig.SETTLEMENT_JOB_NAME) Job settlementTaskletJob,
            @Qualifier(SettlementBatchConfig.SETTLEMENT_CHUNK_JOB_NAME) Job settlementChunkJob) {
        this.jobOperator = jobOperator;
        this.settlementTaskletJob = settlementTaskletJob;
        this.settlementChunkJob = settlementChunkJob;
    }

    @Override
    public JobExecution launchTasklet(LocalDate settlementDate) throws JobInstanceAlreadyCompleteException, InvalidJobParametersException, JobExecutionAlreadyRunningException, JobRestartException {
        return jobOperator.start(settlementTaskletJob, buildJobParameters(settlementDate, "tasklet"));
    }

    @Override
    public JobExecution launchChunk(LocalDate settlementDate) throws JobInstanceAlreadyCompleteException, InvalidJobParametersException, JobExecutionAlreadyRunningException, JobRestartException {
        return jobOperator.start(settlementChunkJob, buildJobParameters(settlementDate, "chunk"));
    }

    private JobParameters buildJobParameters(LocalDate settlementDate, String mode) {
        return new JobParametersBuilder()
                .addString("settlementDate", settlementDate.toString())
                .addString("mode", mode)
                // 동일 날짜 재실행을 위한 유니크 파라미터
                .addLong("requestedAt", System.currentTimeMillis())
                .toJobParameters();
    }
}
