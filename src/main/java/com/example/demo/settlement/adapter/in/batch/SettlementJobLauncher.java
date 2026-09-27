package com.example.demo.settlement.adapter.in.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/** 일일 정산 Job 실행기. 운영에서는 k8s CronJob(1주차 ADR-021)이 이 경로를 호출한다. */
@Service
public class SettlementJobLauncher {

    private final JobOperator jobOperator;
    private final Job dailySettlementJob;

    public SettlementJobLauncher(JobOperator jobOperator,
                                 @Qualifier(DailySettlementJobConfig.JOB_NAME) Job dailySettlementJob) {
        this.jobOperator = jobOperator;
        this.dailySettlementJob = dailySettlementJob;
    }

    /**
     * requestedAt 을 넣어 매 실행을 새 JobInstance 로 만든다(기존 SettlementJobServiceImpl 과 같은 방식).
     * 같은 날짜를 다시 돌려도 안전한 이유는 Job 재시작 기능이 아니라 각 유스케이스의 멱등성이다.
     */
    public JobExecution launch(LocalDate businessDate) throws Exception {
        JobParameters parameters = new JobParametersBuilder()
                .addString(DailySettlementJobConfig.PARAM_BUSINESS_DATE, businessDate.toString())
                .addLong("requestedAt", System.currentTimeMillis())
                .toJobParameters();
        return jobOperator.start(dailySettlementJob, parameters);
    }
}
